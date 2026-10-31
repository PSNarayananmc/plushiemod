package com.redeagle.plushieparty.client;

import com.redeagle.plushieparty.client.render.CreeperPlushieRenderer;
import com.redeagle.plushieparty.client.render.PlushieItemRenderers;
import com.redeagle.plushieparty.registry.PlushieBlocks;
import com.redeagle.plushieparty.registry.PlushieEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey;

/**
 * Client entrypoint: entity renderer, item model override and dance sync
 * receivers.
 */
public class PlushiePartyClient implements ClientModInitializer {
        /** Attached to every {@code LivingEntityRenderState}; non-null while that entity is dancing. */
        public static final RenderStateDataKey<DanceState.Entry> DANCE_STATE_KEY = RenderStateDataKey.create();

        @Override
        public void onInitializeClient() {
                EntityRendererRegistry.register(PlushieEntities.CREEPER_PLUSHIE, CreeperPlushieRenderer::new);

                ModelLoadingPlugin.register(pluginContext -> pluginContext.modifyItemModelAfterBake().register((model, context) -> {
                        if (PlushieBlocks.HITBOX_ID.equals(context.itemId())) {
                                return new PlushieItemRenderers.PlushieItemModel();
                        }
                        return model;
                }));

                DanceState.initNetworking();
        }
}
