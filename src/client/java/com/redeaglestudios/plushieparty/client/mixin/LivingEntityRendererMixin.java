package com.redeaglestudios.plushieparty.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.redeaglestudios.plushieparty.client.DanceManager;
import com.redeaglestudios.plushieparty.client.PlushieAnimations;
import com.redeaglestudios.plushieparty.client.PlushiePartyClient;

/**
 * Makes every dancing living entity (mobs, players AND the plushie itself)
 * perform the party dance by wrapping the whole entity render in the Bedrock
 * root-bone transforms: bounce, 360-degree spin, hop and squash-stretch.
 *
 * This mirrors Bedrock's playAnimation(party_dance) on the root bone; the
 * first-person check is implicit because the local player is not rendered in
 * first person on Java Edition.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    /** render state -> {danceStartTick, danceStopTick}; weak so states can be pooled. */
    @Unique
    private static final java.util.Map<LivingEntityRenderState, long[]> PLUSHIE_STATE_INFO =
        java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void plushieParty$extractDance(LivingEntity entity, LivingEntityRenderState state,
                                           float partialTicks, CallbackInfo ci) {
        long start = DanceManager.danceStartTick(entity.getId());
        long stop = DanceManager.danceStopTick(entity.getId());
        if (start >= 0 || stop >= 0) {
            PLUSHIE_STATE_INFO.put(state, new long[] {start, stop});
        } else {
            PLUSHIE_STATE_INFO.remove(state);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void plushieParty$applyDance(LivingEntityRenderState state, PoseStack poseStack,
                                         SubmitNodeCollector submitNodeCollector, CameraRenderState camera,
                                         CallbackInfo ci) {
        long[] info = PLUSHIE_STATE_INFO.remove(state);
        if (info == null) {
            return;
        }
        long start = info[0];
        long stop = info[1];
        boolean dancing = start >= 0;
        boolean blending = !dancing && stop >= 0
            && (PlushiePartyClient.gameTime() - stop) < (long) (PlushieAnimations.STOP_BLEND_SECONDS * 20f);
        if (!dancing && !blending) {
            return;
        }

        long now = PlushiePartyClient.gameTime();
        float blend = 1.0f;
        if (!dancing) {
            float sinceStop = (now - stop) / 20f;
            blend = 1.0f - Math.min(1.0f, sinceStop / PlushieAnimations.STOP_BLEND_SECONDS);
        }
        if (blend <= 0f) {
            return;
        }

        float seconds = dancing ? Math.max(0f, (now - start) / 20f) : 0f;
        PlushieAnimations.DanceFrame frame = PlushieAnimations.partyDance(seconds);

        // Ease every component back to identity while blending out.
        float rotX = frame.rotXDeg() * blend;
        float rotY = frame.rotYDeg() * blend;
        float rotZ = frame.rotZDeg() * blend;
        float up = frame.upUnits() * blend / 16f;   // model units -> blocks
        float sx = 1f + (frame.scaleX() - 1f) * blend;
        float sy = 1f + (frame.scaleY() - 1f) * blend;
        float sz = 1f + (frame.scaleZ() - 1f) * blend;

        // Origin is the entity's feet (vanilla setupRotations pivots there too).
        poseStack.translate(0f, up, 0f);
        if (rotY != 0f) {
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotY), 0f, 1f, 0f)));
        }
        if (rotX != 0f || rotZ != 0f) {
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotX), 1f, 0f, 0f)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotZ), 0f, 0f, 1f)));
        }
        poseStack.scale(sx, sy, sz);
    }
}
