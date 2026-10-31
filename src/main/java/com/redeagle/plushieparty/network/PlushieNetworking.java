package com.redeagle.plushieparty.network;

import com.redeagle.plushieparty.PlushiePartyMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

/**
 * S2C payloads that drive the client-side dance animation.
 *
 * <p>Bedrock played the dance animations directly on the entities via
 * {@code playAnimation}; Java needs an explicit sync. Start packets carry the
 * server's game time so that every client animates from the same beat.</p>
 */
public final class PlushieNetworking {
        public record StartDancePayload(int entityId, long startGameTime) implements CustomPacketPayload {
                public static final Type<StartDancePayload> TYPE = new Type<>(PlushiePartyMod.id("start_dance"));
                public static final StreamCodec<RegistryFriendlyByteBuf, StartDancePayload> CODEC = StreamCodec.composite(
                                ByteBufCodecs.VAR_INT, StartDancePayload::entityId,
                                ByteBufCodecs.VAR_LONG, StartDancePayload::startGameTime,
                                StartDancePayload::new);

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        public record StopDancePayload(int entityId) implements CustomPacketPayload {
                public static final Type<StopDancePayload> TYPE = new Type<>(PlushiePartyMod.id("stop_dance"));
                public static final StreamCodec<RegistryFriendlyByteBuf, StopDancePayload> CODEC = StreamCodec.composite(
                                ByteBufCodecs.VAR_INT, StopDancePayload::entityId,
                                StopDancePayload::new);

                @Override
                public Type<? extends CustomPacketPayload> type() {
                        return TYPE;
                }
        }

        static {
                PayloadTypeRegistry.clientboundPlay().register(StartDancePayload.TYPE, StartDancePayload.CODEC);
                PayloadTypeRegistry.clientboundPlay().register(StopDancePayload.TYPE, StopDancePayload.CODEC);
        }

        public static void init() {
                // Class loading triggers registration.
        }

        public static void sendStartDance(ServerPlayer player, StartDancePayload payload) {
                ServerPlayNetworking.send(player, payload);
        }

        public static void sendStopDance(ServerPlayer player, StopDancePayload payload) {
                ServerPlayNetworking.send(player, payload);
        }

        private PlushieNetworking() {
        }
}
