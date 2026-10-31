package com.redeagle.plushieparty.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.redeagle.plushieparty.PlushiePartyMod;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Item display for the Creeper Plushie block item.
 *
 * <p>The Bedrock block uses {@code minecraft:item_visual} with the dedicated
 * {@code geometry.re_plushie_party.creeper_plushie_thumbnail} (the same doll,
 * rotated 180&deg;) as its inventory/ground model. This is recreated here with
 * a custom item model + special model renderer pair, so the item shows the
 * real animated-doll geometry instead of a flat sprite. The leg rotations of
 * the thumbnail (70&deg;) cannot be expressed in vanilla block model JSON,
 * which is why the geometry is emitted directly.</p>
 */
public final class PlushieItemRenderers {
        public static final Identifier TEXTURE = PlushiePartyMod.id("textures/entity/creeper_plushie.png");

        /** Renders the doll rotated 180 (the thumbnail pose) for all item display contexts. */
        public static final SpecialModelRenderer<Void> PLUSHIE_RENDERER = new SpecialModelRenderer<>() {
                @Override
                public void submit(@Nullable Void argument, PoseStack poseStack, SubmitNodeCollector collector,
                                int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
                        poseStack.pushPose();
                        // Thumbnail root bone: rotation [0, 180, 0]; sit the doll in the 0..1 item cube.
                        poseStack.translate(0.5F, 0.0F, 0.5F);
                        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                        // Our mesh data is authored in Java model space (y down); flip it into
                        // the direct Bedrock space this pose stack uses, with the same
                        // -1.501 ground offset as the entity chain so the doll stands on the
                        // bottom of the 0..1 item cube (like vanilla block items).
                        poseStack.scale(-1.0F, -1.0F, 1.0F);
                        poseStack.translate(0.0F, -1.501F, 0.0F);
                        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) ->
                                        PlushieMeshRenderer.emit(pose, buffer, lightCoords, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F));
                        poseStack.popPose();
                }

                @Override
                public void getExtents(Consumer<Vector3fc> output) {
                        // Bounding corners of the doll after the ground offset (10/16 tall,
                        // splayed legs to ±5/16).
                        output.accept(new Vector3f(0.3125F, 0.625F, 0.3125F));
                        output.accept(new Vector3f(-0.3125F, 0.0F, -0.3125F));
                }

                @Override
                public @Nullable Void extractArgument(ItemStack stack) {
                        return null;
                }
        };

        /** Item model replacement bound through the Fabric model loading pipeline. */
        public static final class PlushieItemModel implements ItemModel {
                @Override
                public void update(ItemStackRenderState state, ItemStack stack, ItemModelResolver resolver,
                                ItemDisplayContext displayContext, net.minecraft.client.multiplayer.ClientLevel level,
                                net.minecraft.world.entity.ItemOwner owner, int seed) {
                        var layer = state.newLayer();
                        layer.setupSpecialModel(PLUSHIE_RENDERER, null);
                        layer.setItemTransform(transformFor(displayContext));
                        layer.setUsesBlockLight(false);
                }

                private static ItemTransform transformFor(ItemDisplayContext context) {
                        return switch (context) {
                                case GUI -> new ItemTransform(vec(30.0F, 225.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.625F, 0.625F, 0.625F));
                                case GROUND -> new ItemTransform(vec(0, 0, 0), vec(0.0F, 3.0F / 16.0F, 0.0F), vec(0.25F, 0.25F, 0.25F));
                                case FIXED -> new ItemTransform(vec(0, 0, 0), vec(0, 0, 0), vec(0.5F, 0.5F, 0.5F));
                                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                                                new ItemTransform(vec(75.0F, 45.0F, 0.0F), vec(0.0F, 2.5F / 16.0F, 0.0F), vec(0.375F, 0.375F, 0.375F));
                                case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND ->
                                                new ItemTransform(vec(0.0F, 45.0F, 0.0F), vec(0.0F, 0.0F, 0.0F), vec(0.40F, 0.40F, 0.40F));
                                case HEAD -> new ItemTransform(vec(0, 0, 0), vec(0.0F, 0.0F, 0.0F), vec(1.0F, 1.0F, 1.0F));
                                default -> ItemTransform.NO_TRANSFORM;
                        };
                }

                private static Vector3f vec(float x, float y, float z) {
                        return new Vector3f(x, y, z);
                }
        }

        private PlushieItemRenderers() {
        }
}
