package com.redeaglestudios.plushieparty.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side dance state, driven by the server's 74-tick dancer sync payloads.
 * Phase is computed from the (synced) level game time, so every client shows
 * the same beat without extra network traffic.
 */
public final class DanceManager {
    private DanceManager() {}

    /** entityId -> game time tick at which the dance started. */
    private static final Map<Integer, Long> DANCE_START = new HashMap<>();
    /** entityId -> game time tick at which the dance was stopped (blend-out window). */
    private static final Map<Integer, Long> DANCE_STOP = new HashMap<>();

    public static void handleStart(int[] entityIds) {
        long now = gameTime();
        for (int id : entityIds) {
            DANCE_START.put(id, now);
            DANCE_STOP.remove(id);
        }
    }

    public static void handleStop(int[] entityIds) {
        long now = gameTime();
        for (int id : entityIds) {
            DANCE_START.remove(id);
            DANCE_STOP.put(id, now);
        }
    }

    /** @return the game tick at which the entity started dancing, or -1. */
    public static long danceStartTick(int entityId) {
        Long start = DANCE_START.get(entityId);
        return start == null ? -1L : start;
    }

    /** @return the game tick at which the entity stopped dancing, or -1. */
    public static long danceStopTick(int entityId) {
        Long stop = DANCE_STOP.get(entityId);
        return stop == null ? -1L : stop;
    }

    public static boolean isDancing(int entityId) {
        return DANCE_START.containsKey(entityId);
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getGameTime() : 0L;
    }

    /** Drops stop-blend entries older than 10 seconds to keep the maps small. */
    public static void cleanup() {
        long now = gameTime();
        DANCE_STOP.values().removeIf(stop -> now - stop > 200L);
        DANCE_START.entrySet().removeIf(e -> {
            Entity e2 = Minecraft.getInstance().level == null
                ? null : Minecraft.getInstance().level.getEntity(e.getKey());
            return e2 == null && now - e.getValue() > 600L;
        });
    }
}
