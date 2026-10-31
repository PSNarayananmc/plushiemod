package com.redeaglestudios.plushieparty.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import com.redeaglestudios.plushieparty.ModPayloads;
import com.redeaglestudios.plushieparty.ModRegistry;
import com.redeaglestudios.plushieparty.PlushieParty;

public class PlushiePartyClient implements ClientModInitializer {
        @Override
        public void onInitializeClient() {
                // Doll renderer (custom geometry + squeeze + party dance).
                EntityRendererRegistry.register(ModRegistry.CREEPER_PLUSHIE, CreeperPlushieRenderer::new);

                // 74-tick dance sync from the server.
                ClientPlayNetworking.registerGlobalReceiver(ModPayloads.DanceStart.TYPE, (payload, context) ->
                        context.client().execute(() -> DanceManager.handleStart(payload.entityIds())));
                ClientPlayNetworking.registerGlobalReceiver(ModPayloads.DanceStop.TYPE, (payload, context) ->
                        context.client().execute(() -> DanceManager.handleStop(payload.entityIds())));

                PlushieParty.LOGGER.info("Plushie Party client ready");
        }

        public static long gameTime() {
                var mc = net.minecraft.client.Minecraft.getInstance();
                return mc.level != null ? mc.level.getGameTime() : 0L;
        }

        /** Convenience id helper for client classes. */
        public static net.minecraft.resources.Identifier id(String path) {
                return PlushieParty.id(path);
        }
}
