package com.redeagle.plushieparty.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side dance state, filled by the server's dance packets.
 *
 * <p>Bedrock called {@code playAnimation} on each dancer directly; Java
 * tracks (per entity id) when the dance started on the server clock so every
 * client renders the same beat of the 3.7 s loop.</p>
 */
public final class DanceState {
        /** @param entityId the dancing entity's id (render states carry no entity reference).
         *  @param startTicks server game time when the dance started.
         *  @param stopTicks  server game time when the dance stopped, or -1 while active. */
        public record Entry(int entityId, long startTicks, long stopTicks) {
        }

        private static final Map<Integer, Entry> DANCING = new ConcurrentHashMap<>();

        public static void start(int entityId, long startGameTime) {
                DANCING.put(entityId, new Entry(entityId, startGameTime, -1L));
        }

        public static void stop(int entityId, long stopGameTime) {
                Entry existing = DANCING.get(entityId);
                if (existing != null && existing.stopTicks() < 0) {
                        DANCING.put(entityId, new Entry(entityId, existing.startTicks(), stopGameTime));
                }
        }

        /** @return the dance entry, or null if not dancing / fully blended out. */
        public static Entry get(int entityId) {
                Entry entry = DANCING.get(entityId);
                if (entry == null) {
                        return null;
                }
                if (entry.stopTicks() >= 0) {
                        long now = nowTicks();
                        // Fully blended out (0.25 s blendOutTime) -> drop the entry.
                        if ((now - entry.stopTicks()) / 20.0D > com.redeagle.plushieparty.PartyDanceCurves.BLEND_SECONDS) {
                                DANCING.remove(entityId);
                                return null;
                        }
                }
                return entry;
        }

        public static long nowTicks() {
                Minecraft minecraft = Minecraft.getInstance();
                return minecraft.level != null ? minecraft.level.getGameTime() : 0L;
        }

        public static void clear() {
                DANCING.clear();
        }

        public static void initNetworking() {
                ClientPlayNetworking.registerGlobalReceiver(com.redeagle.plushieparty.network.PlushieNetworking.StartDancePayload.TYPE,
                                (payload, context) -> DanceState.start(payload.entityId(), payload.startGameTime()));
                ClientPlayNetworking.registerGlobalReceiver(com.redeagle.plushieparty.network.PlushieNetworking.StopDancePayload.TYPE,
                                (payload, context) -> DanceState.stop(payload.entityId(), nowTicks()));
                ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> DanceState.clear());
        }

        private DanceState() {
        }
}
