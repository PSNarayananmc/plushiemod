package com.redeaglestudios.plushieparty;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Clientbound dance sync payloads, mirroring the Bedrock 74-tick dancer sync:
 * the server tells clients which entities start dancing (party started or a
 * newcomer entered the 16-block radius) and which stopped (left the radius or
 * the party ended). Dance phase is timed client-side from the level game time.
 *
 * Entity ids are the network entity ids (Entity#getId), resolved client-side
 * with ClientLevel#getEntity(int).
 */
public final class ModPayloads {
    private ModPayloads() {}

    public record DanceStart(int[] entityIds) implements CustomPacketPayload {
        public static final Type<DanceStart> TYPE = new Type<>(PlushieParty.id("dance_start"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DanceStart> CODEC =
            StreamCodec.of((buf, p) -> writeIds(buf, p.entityIds()), buf -> new DanceStart(readIds(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record DanceStop(int[] entityIds) implements CustomPacketPayload {
        public static final Type<DanceStop> TYPE = new Type<>(PlushieParty.id("dance_stop"));
        public static final StreamCodec<RegistryFriendlyByteBuf, DanceStop> CODEC =
            StreamCodec.of((buf, p) -> writeIds(buf, p.entityIds()), buf -> new DanceStop(readIds(buf)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void writeIds(RegistryFriendlyByteBuf buf, int[] ids) {
        ((FriendlyByteBuf) buf).writeVarIntArray(ids);
    }

    private static int[] readIds(RegistryFriendlyByteBuf buf) {
        return ((FriendlyByteBuf) buf).readVarIntArray();
    }

    public static DanceStart danceStart(int[] entityIds) {
        return new DanceStart(entityIds);
    }

    public static DanceStop danceStop(int[] entityIds) {
        return new DanceStop(entityIds);
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(DanceStart.TYPE, DanceStart.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(DanceStop.TYPE, DanceStop.CODEC);
    }
}
