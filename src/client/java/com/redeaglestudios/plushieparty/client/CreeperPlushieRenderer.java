package com.redeaglestudios.plushieparty.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

import com.redeaglestudios.plushieparty.CreeperPlushieEntity;

/**
 * Renderer for the Creeper Plushie doll. No shadow (Bedrock doll has none of
 * note and zero collision), custom model, squeeze animation in the model.
 * The party dance is applied by the shared LivingEntityRenderer mixin.
 */
public class CreeperPlushieRenderer extends MobRenderer<CreeperPlushieEntity, CreeperPlushieRenderState, CreeperPlushieModel> {

    public CreeperPlushieRenderer(EntityRendererProvider.Context context) {
        super(context, new CreeperPlushieModel(PlushieGeometry.bakeRoot()), 0.0f);
    }

    @Override
    public CreeperPlushieRenderState createRenderState() {
        return new CreeperPlushieRenderState();
    }

    @Override
    public void extractRenderState(CreeperPlushieEntity entity, CreeperPlushieRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (entity.getSqueezeTicksLeft() > 0) {
            if (entity.clientSqueezeStartAge < 0f || !entity.clientSqueezeActive) {
                entity.clientSqueezeStartAge = entity.tickCount + partialTicks;
                entity.clientSqueezeActive = true;
            }
            state.squeezeStartAge = entity.clientSqueezeStartAge;
        } else {
            entity.clientSqueezeActive = false;
            state.squeezeStartAge = -1f;
        }
    }

    @Override
    public Identifier getTextureLocation(CreeperPlushieRenderState state) {
        return CreeperPlushieModel.texture();
    }
}
