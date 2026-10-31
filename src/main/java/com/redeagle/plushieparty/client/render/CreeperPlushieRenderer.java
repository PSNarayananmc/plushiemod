package com.redeagle.plushieparty.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.redeagle.plushieparty.PartyDanceCurves;
import com.redeagle.plushieparty.PlushiePartyMod;
import com.redeagle.plushieparty.client.DanceState;
import com.redeagle.plushieparty.entity.CreeperPlushieEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Renders the Creeper Plushie entity with the exact original geometry and the
 * two animation systems of the addon:
 *
 * <ul>
 *   <li><b>Click squish</b> ({@code plushie_click_squeak}): 0.5 s of keyframed
 *       body scale, plus the toy squeak sound (played server-side at the same
 *       moment the Bedrock animation's sound effect would fire)</li>
 *   <li><b>Party dance</b> ({@code party_dance}): the full Molang curve port,
 *       applied to the whole model like the Bedrock root bone</li>
 * </ul>
 */
public class CreeperPlushieRenderer extends EntityRenderer<CreeperPlushieEntity, CreeperPlushieRenderer.PlushieRenderState> {
        public static final Identifier TEXTURE = PlushiePartyMod.id("textures/entity/creeper_plushie.png");

        public static class PlushieRenderState extends EntityRenderState {
                public boolean squishing;
                public long squishStartGameTime;
                public long danceStartTicks = -1L;
                public long danceStopTicks = -1L;
                public float yaw;
        }

        public CreeperPlushieRenderer(EntityRendererProvider.Context context) {
                super(context);
                this.shadowRadius = 0.0F;
        }

        @Override
        public PlushieRenderState createRenderState() {
                return new PlushieRenderState();
        }

        @Override
        public void extractRenderState(CreeperPlushieEntity entity, PlushieRenderState state, float partialTick) {
                super.extractRenderState(entity, state, partialTick);
                state.yaw = entity.getYRot();
                state.squishing = entity.isSquishing();
                state.squishStartGameTime = entity.squishStartGameTime();
                DanceState.Entry dance = DanceState.get(entity.getId());
                if (dance != null) {
                        state.danceStartTicks = dance.startTicks();
                        state.danceStopTicks = dance.stopTicks();
                } else {
                        state.danceStartTicks = -1L;
                        state.danceStopTicks = -1L;
                }
        }

        @Override
        public void submit(PlushieRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                        net.minecraft.client.renderer.state.level.CameraRenderState camera) {
                float partial = state.ageInTicks % 1.0F;
                double now = DanceState.nowTicks() + partial;

                poseStack.pushPose();
                // Same convention as vanilla living renderers: 180 - yaw, model front on -Z.
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - Mth.wrapDegrees(state.yaw)));

                // Party dance on the whole model (pre-flip space = Bedrock bone space).
                if (state.danceStartTicks >= 0L) {
                        double blend = PartyDanceCurves.blendFor((long) now, state.danceStartTicks, state.danceStopTicks);
                        if (blend > 0.0D) {
                                double seconds = (now - state.danceStartTicks) / 20.0D;
                                PartyDanceCurves.applyPreFlip(poseStack, PartyDanceCurves.evaluate(seconds, blend));
                        }
                }

                poseStack.scale(-1.0F, -1.0F, 1.0F);
                poseStack.translate(0.0F, -1.501F, 0.0F);

                float bodyScaleX = 1.0F;
                float bodyScaleY = 1.0F;
                float bodyScaleZ = 1.0F;
                if (state.squishing) {
                        double seconds = Math.max(0.0D, (now - state.squishStartGameTime) / 20.0D);
                        float[] scale = evaluateSquish(seconds);
                        bodyScaleX = scale[0];
                        bodyScaleY = scale[1];
                        bodyScaleZ = scale[2];
                }
                final float fsx = bodyScaleX;
                final float fsy = bodyScaleY;
                final float fsz = bodyScaleZ;

                collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) ->
                                PlushieMeshRenderer.emit(pose, buffer, state.lightCoords, OverlayTexture.NO_OVERLAY, fsx, fsy, fsz));

                poseStack.popPose();
                super.submit(state, poseStack, collector, camera);
        }

        /**
         * The 13 keyframes of {@code animation.re_plushie_party.plushie_click_squeak}
         * (0.5 s, linear interpolation, hold on last frame).
         */
        private static float[] evaluateSquish(double seconds) {
                double t = Mth.clamp(seconds, 0.0D, 0.5D);
                int i = 0;
                while (i < KEYS.length - 2 && KEYS[i + 1][0] < t) {
                        i++;
                }
                double t0 = KEYS[i][0];
                double t1 = KEYS[i + 1][0];
                float delta = t1 > t0 ? (float) ((t - t0) / (t1 - t0)) : 0.0F;
                return new float[] {
                                Mth.lerp(delta, (float) KEYS[i][1], (float) KEYS[i + 1][1]),
                                Mth.lerp(delta, (float) KEYS[i][2], (float) KEYS[i + 1][2]),
                                Mth.lerp(delta, (float) KEYS[i][3], (float) KEYS[i + 1][3]),
                };
        }

        /** {time, scaleX, scaleY, scaleZ} keyframes, copied verbatim from the animation JSON. */
        private static final double[][] KEYS = {
                        { 0.0, 1.0, 1.0, 1.0 },
                        { 0.0417, 1.0178, 0.9131, 1.0163 },
                        { 0.0833, 1.05, 0.8042, 1.05 },
                        { 0.125, 1.0822, 0.7499, 1.0837 },
                        { 0.1667, 1.1, 0.8, 1.1 },
                        { 0.2083, 1.0644, 0.9534, 1.0674 },
                        { 0.25, 1.0, 1.1254, 1.0 },
                        { 0.2917, 0.9356, 1.2231, 0.9326 },
                        { 0.3333, 0.9, 1.2, 0.9 },
                        { 0.375, 0.9178, 1.1266, 0.9163 },
                        { 0.4167, 0.95, 1.0663, 0.95 },
                        { 0.4583, 0.9822, 1.0212, 0.9837 },
                        { 0.5, 1.0, 1.0, 1.0 },
        };
}
