package com.redeagle.plushieparty.party;

import com.redeagle.plushieparty.network.PlushieNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative party system - a faithful port of the Bedrock
 * {@code scripts/main.js} party logic.
 *
 * <p>Behaviour reproduced 1:1:</p>
 * <ul>
 *   <li>right-clicking a plushie host with a <b>stick</b> or any
 *       <b>music disc</b> in the main hand toggles a party; clicking without
 *       one only squishes (handled by the block) and never toggles</li>
 *   <li>a sync pass runs immediately when a party starts and then every
 *       {@value #BAR_TICKS} ticks ({@code system.runInterval(74)})</li>
 *   <li>each sync gathers every player and every mob within
 *       {@value #PARTY_RADIUS} blocks of the host (plus the host itself) and
 *       makes newcomers start dancing; dancers that left every party radius
 *       stop dancing</li>
 *   <li>multiple parties merge naturally - a dancer only stops once it is
 *       outside <i>all</i> party radii, and stopping one party never stops
 *       dancers that are still inside another</li>
 *   <li>when the last party ends the ticker stops ({@code system.clearRun})</li>
 *   <li>removed hosts/dancers are pruned (the Bedrock
 *       {@code world.afterEvents.entityRemove} handler plus the
 *       {@code isValid} checks inside the sync pass)</li>
 * </ul>
 */
public final class PartyManager {
        public static final int BAR_TICKS = 74;
        public static final double PARTY_RADIUS = 16.0D;

        /** Host entity (the plushie that was clicked) per party. */
        private static final Map<UUID, Entity> PARTIES = new HashMap<>();
        /** Everyone currently dancing (host plushies, mobs and players). */
        private static final Map<UUID, Entity> DANCERS = new HashMap<>();
        /** Ticks until the next sync pass; -1 while no party interval is running. */
        private static int ticksUntilSync = -1;
        private static boolean tickerRegistered = false;

        private PartyManager() {
        }

        /**
         * Toggles a party hosted by the given plushie. Reproduces
         * {@code toggleParty(player, host)} from the original script.
         */
        public static void toggleParty(Player player, Entity host) {
                if (player.level().isClientSide()) {
                        return;
                }
                if (PARTIES.containsKey(host.getUUID())) {
                        PARTIES.remove(host.getUUID());
                        syncDancers(false);
                        return;
                }
                ItemStack mainHand = player.getMainHandItem();
                if (mainHand.isEmpty()) {
                        return;
                }
                boolean isStick = mainHand.is(Items.STICK);
                // Bedrock: item.typeId.startsWith("minecraft:music_disc_") - kept identical.
                boolean isMusicDisc = isVanillaMusicDisc(mainHand);
                if (!isStick && !isMusicDisc) {
                        return;
                }
                PARTIES.put(host.getUUID(), host);
                ensureTicker();
                ticksUntilSync = BAR_TICKS;
                syncDancers(true);
        }

        /** Immediate cleanup when a host or dancer entity is removed (Bedrock entityRemove event). */
        public static void onEntityRemoved(Entity entity) {
                PARTIES.remove(entity.getUUID());
                DANCERS.remove(entity.getUUID());
        }

        private static void ensureTicker() {
                if (!tickerRegistered) {
                        tickerRegistered = true;
                        ServerTickEvents.END_SERVER_TICK.register(server -> onServerTick());
                }
        }

        /** Faithful port of {@code item.typeId.startsWith(MUSIC_DISC_PREFIX)}. */
        private static boolean isVanillaMusicDisc(ItemStack stack) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return itemId.getNamespace().equals("minecraft") && itemId.getPath().startsWith("music_disc_");
        }

        /** {@code system.runInterval(() -> syncDancers(true), BAR_TICKS)} equivalent. */
        private static void onServerTick() {
                if (ticksUntilSync > 0) {
                        ticksUntilSync--;
                        if (ticksUntilSync == 0) {
                                syncDancers(true);
                                if (!PARTIES.isEmpty()) {
                                        ticksUntilSync = BAR_TICKS;
                                }
                        }
                }
        }

        /** Port of {@code syncDancers(startNewcomers)}. */
        private static void syncDancers(boolean startNewcomers) {
                Set<UUID> guests = new HashSet<>();

                Iterator<Map.Entry<UUID, Entity>> partyIterator = PARTIES.entrySet().iterator();
                while (partyIterator.hasNext()) {
                        Map.Entry<UUID, Entity> entry = partyIterator.next();
                        Entity plushie = entry.getValue();
                        if (!plushie.isAlive() || plushie.isRemoved()) {
                                partyIterator.remove();
                                continue;
                        }
                        if (!(plushie.level() instanceof ServerLevel level)) {
                                partyIterator.remove();
                                continue;
                        }
                        Vec3 center = plushie.position();
                        double radiusSq = PARTY_RADIUS * PARTY_RADIUS;
                        AABB searchBox = new AABB(
                                        center.x - PARTY_RADIUS, center.y - PARTY_RADIUS, center.z - PARTY_RADIUS,
                                        center.x + PARTY_RADIUS, center.y + PARTY_RADIUS, center.z + PARTY_RADIUS);

                        // Bedrock: dimension.getEntities({...maxDistance: 16, families: ["mob"]})
                        List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, searchBox,
                                        e -> e.isAlive() && e.distanceToSqr(center) <= radiusSq);
                        // Bedrock: dimension.getPlayers({...maxDistance: 16})
                        List<ServerPlayer> nearbyPlayers = level.players().stream()
                                        .filter(p -> p.isAlive() && p.distanceToSqr(center) <= radiusSq)
                                        .toList();

                        for (Entity guest : nearbyMobs) {
                                addGuest(guests, guest, startNewcomers);
                        }
                        for (Entity guest : nearbyPlayers) {
                                addGuest(guests, guest, startNewcomers);
                        }
                        // Bedrock: nearby.push(plushie) - the host always dances too.
                        addGuest(guests, plushie, startNewcomers);
                }

                // Stop everyone who is no longer inside any party radius.
                Iterator<Map.Entry<UUID, Entity>> dancerIterator = DANCERS.entrySet().iterator();
                while (dancerIterator.hasNext()) {
                        Map.Entry<UUID, Entity> entry = dancerIterator.next();
                        if (guests.contains(entry.getKey())) {
                                continue;
                        }
                        Entity dancer = entry.getValue();
                        dancerIterator.remove();
                        if (dancer.isAlive() && !dancer.isRemoved()) {
                                broadcastStop(dancer);
                        }
                }

                // Bedrock: if no parties remain, system.clearRun the interval.
                if (PARTIES.isEmpty()) {
                        ticksUntilSync = -1;
                }
        }

        private static void addGuest(Set<UUID> guests, Entity guest, boolean startNewcomers) {
                guests.add(guest.getUUID());
                if (!startNewcomers || DANCERS.containsKey(guest.getUUID())) {
                        return;
                }
                DANCERS.put(guest.getUUID(), guest);
                broadcastStart(guest);
        }

        /** Tells every client that can see the dancer to start the dance animation. */
        private static void broadcastStart(Entity dancer) {
                long startGameTime = dancer.level().getGameTime();
                PlushieNetworking.StartDancePayload payload = new PlushieNetworking.StartDancePayload(dancer.getId(), startGameTime);
                for (ServerPlayer player : PlayerLookup.tracking(dancer)) {
                        PlushieNetworking.sendStartDance(player, payload);
                }
                if (dancer instanceof ServerPlayer self) {
                        // The dancer's own client needs it too (third-person view).
                        PlushieNetworking.sendStartDance(self, payload);
                }
        }

        private static void broadcastStop(Entity dancer) {
                PlushieNetworking.StopDancePayload payload = new PlushieNetworking.StopDancePayload(dancer.getId());
                for (ServerPlayer player : PlayerLookup.tracking(dancer)) {
                        PlushieNetworking.sendStopDance(player, payload);
                }
                if (dancer instanceof ServerPlayer self) {
                        PlushieNetworking.sendStopDance(self, payload);
                }
        }

        /** @return true if the given entity is currently hosting a party. */
        public static boolean isHosting(Entity entity) {
                return PARTIES.containsKey(entity.getUUID());
        }

        /** Debug helper: number of running parties. */
        public static int partyCount() {
                return PARTIES.size();
        }
}
