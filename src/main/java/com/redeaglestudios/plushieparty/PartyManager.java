package com.redeaglestudios.plushieparty;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.phys.AABB;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-authoritative party system - faithful port of the Bedrock scripts/main.js.
 *
 * - Right-click a plushie with a stick or any music disc to toggle its party.
 * - Every player and mob within 16 blocks dances, plus the plushie itself.
 * - Dancer sets are re-synced every 74 ticks (BAR_TICKS); newcomers start dancing,
 *   guests that left the radius get the stop animation (0.25s blend-out).
 * - Multiple parties can run at once; stopping one never stops the others.
 * - Disc music loops until the party stops (stick parties play the silent, resource
 *   pack replaceable "party_music" slot once, exactly like Bedrock).
 */
public final class PartyManager {
    public static final int BAR_TICKS = 74;
    public static final int PARTY_RADIUS = 16;

    /** One active party hosted by a plushie entity. */
    private static final class Party {
        final UUID hostId;
        final ServerLevel level;
        boolean loopMusic = false;
        Holder<SoundEvent> song = null;
        float songLengthSeconds = 0f;
        long nextSongTick = 0L;

        Party(UUID hostId, ServerLevel level) {
            this.hostId = hostId;
            this.level = level;
        }
    }

    private static PartyManager instance;

    private final Map<UUID, Party> parties = new HashMap<>();
    /** Entity ids that were last announced as dancing (union over all parties). */
    private final Set<UUID> dancerIds = new HashSet<>();
    private long tickCounter = 0L;

    public PartyManager() {
        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);
    }

    public static void init() {
        instance = new PartyManager();
    }

    public static PartyManager get() {
        return instance;
    }

    // ----------------------------------------------------------------- tick
    private void onServerTick(MinecraftServer server) {
        if (parties.isEmpty()) {
            return;
        }
        tickCounter++;
        if (tickCounter % BAR_TICKS == 0) {
            syncDancers(server, true);
        }
        tickMusic(server);
    }

    private void tickMusic(MinecraftServer server) {
        long gameTime = server.overworld().getGameTime();
        for (Party party : parties.values()) {
            if (party.song == null || party.level == null) {
                continue;
            }
            if (gameTime >= party.nextSongTick) {
                Entity host = party.level.getEntity(party.hostId) instanceof Entity e ? e : null;
                if (host == null) {
                    continue;
                }
                // Vanilla jukebox volume (4.0) so the whole party hears it.
                party.level.playSound(null, host.blockPosition(),
                    party.song.value(), SoundSource.RECORDS, 4.0f, 1.0f);
                int lengthInTicks = Math.max(20, (int) (party.songLengthSeconds * 20.0f));
                party.nextSongTick = gameTime + lengthInTicks;
            }
        }
    }

    // --------------------------------------------------------------- toggle
    public void toggleParty(ServerPlayer player, CreeperPlushieEntity host, ServerLevel level) {
        if (player == null || !player.isAlive() || host == null || !host.isAlive()) {
            return;
        }
        if (parties.containsKey(host.getUUID())) {
            // Any click stops an ongoing party, whatever is in the hand (main.js behavior).
            parties.remove(host.getUUID());
            syncDancers(level.getServer(), false);
            return;
        }

        // Starting a party requires a stick or any music disc in the main hand.
        ItemStack mainHand = player.getMainHandItem();
        boolean isDisc = JukeboxSong.fromStack(mainHand).isPresent();
        if (!mainHand.is(net.minecraft.world.item.Items.STICK) && !isDisc) {
            return;
        }

        Party party = new Party(host.getUUID(), level);
        // Disc music loops until the party stops (CurseForge description).
        net.minecraft.core.Holder<JukeboxSong> songHolder = JukeboxSong.fromStack(mainHand).orElse(null);
        if (songHolder != null) {
            party.song = songHolder.value().soundEvent();
            party.songLengthSeconds = songHolder.value().lengthInSeconds();
            party.loopMusic = true;
        } else {
            // Stick: play the (silent by default) replaceable party music slot once.
            party.song = Holder.direct(ModRegistry.PARTY_MUSIC);
            party.songLengthSeconds = 0f;
            party.loopMusic = false;
            level.playSound(null, host.blockPosition(),
                ModRegistry.PARTY_MUSIC, SoundSource.RECORDS, 4.0f, 1.0f);
        }

        parties.put(host.getUUID(), party);
        syncDancers(level.getServer(), true);
    }

    /** Called when a plushie block/entity is removed (break, orphan cleanup...). */
    public void onPlushieRemoved(Entity host) {
        if (host == null || host.level().isClientSide()) {
            return;
        }
        if (parties.remove(host.getUUID()) != null && host.level() instanceof ServerLevel level) {
            syncDancers(level.getServer(), false);
        }
    }

    // ----------------------------------------------------------------- sync
    private void syncDancers(MinecraftServer server, boolean startNewcomers) {
        Set<UUID> guests = new HashSet<>();
        List<Integer> newcomerIds = new ArrayList<>();

        for (var iterator = parties.values().iterator(); iterator.hasNext(); ) {
            Party party = iterator.next();
            Entity host = party.level.getEntity(party.hostId) instanceof Entity e ? e : null;
            if (host == null || !host.isAlive()) {
                iterator.remove();
                continue;
            }
            AABB radius = host.getBoundingBox().inflate(PARTY_RADIUS, PARTY_RADIUS, PARTY_RADIUS);
            List<Entity> nearby = new ArrayList<>(party.level.getEntitiesOfClass(Mob.class, radius));
            for (ServerPlayer p : party.level.players()) {
                if (radius.intersects(p.getBoundingBox())) {
                    nearby.add(p);
                }
            }
            nearby.add(host);   // the plushie dances too

            for (Entity guest : nearby) {
                guests.add(guest.getUUID());
                if (!dancerIds.contains(guest.getUUID())) {
                    newcomerIds.add(guest.getId());   // network id for the client
                }
            }
        }

        // Guests that are no longer part of any party get the stop animation.
        List<Integer> leaverIds = new ArrayList<>();
        for (Entity guest : collectKnownDancers(server)) {
            if (!guests.contains(guest.getUUID())) {
                leaverIds.add(guest.getId());
            }
        }

        dancerIds.clear();
        dancerIds.addAll(guests);

        if (startNewcomers && !newcomerIds.isEmpty()) {
            broadcastToGuests(server, ModPayloads.danceStart(newcomerIds.stream().mapToInt(Integer::intValue).toArray()));
        }
        if (!leaverIds.isEmpty()) {
            broadcastToGuests(server, ModPayloads.danceStop(leaverIds.stream().mapToInt(Integer::intValue).toArray()));
        }
    }

    /** Best-effort re-location of the dancers we told clients about last time. */
    private List<Entity> collectKnownDancers(MinecraftServer server) {
        List<Entity> found = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity e : level.getAllEntities()) {
                if (dancerIds.contains(e.getUUID())) {
                    found.add(e);
                }
            }
        }
        return found;
    }

    /** Sends a dance payload to every player (client ignores unknown/stale ids). */
    private void broadcastToGuests(MinecraftServer server, CustomPacketPayload payload) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
        }
    }

    public boolean isPartyHost(Entity entity) {
        return entity != null && parties.containsKey(entity.getUUID());
    }
}
