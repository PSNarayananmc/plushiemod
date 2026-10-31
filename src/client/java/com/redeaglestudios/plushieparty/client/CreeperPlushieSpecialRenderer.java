package com.redeaglestudios.plushieparty.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.NoDataSpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;

import org.joml.Vector3fc;

import java.util.function.Consumer;

/**
 * 3D item form renderer for the Creeper Plushie block item - the equivalent of
 * the Bedrock "minecraft:item_visual" thumbnail geometry. Registered as a
 * custom "minecraft:special" item model renderer type via the client mixin.
 */
public class CreeperPlushieSpecialRenderer implements NoDataSpecialModelRenderer {

    private final CreeperPlushieModel model;

    public CreeperPlushieSpecialRenderer(CreeperPlushieModel model) {
        this.model = model;
    }

    @Override
    public void submit(PoseStack poseStack,
                       SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords,
                       boolean hasFoil, int outlineColor) {
        poseStack.pushPose();
        // The model is built around the ground line (model y=24). Translate so the
        // doll stands centered in the item's unit cube, feet on the bottom face.
        poseStack.translate(0.5f, 0.0f, 0.5f);
        submitNodeCollector.submitModelPart(
            this.model.root(),
            poseStack,
            RenderTypes.entityCutout(CreeperPlushieModel.texture()),
            lightCoords,
            overlayCoords,
            null
        );
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        // Model is 10/16 tall, 6/16 wide/deep around the center.
        output.accept(new org.joml.Vector3f(0.1875f, 0.625f, 0.1875f));
    }

    /** Unbaked entry for the SpecialModelRenderers id mapper. */
    public record Unbaked() implements SpecialModelRenderer.Unbaked<Void> {
        public static final CreeperPlushieSpecialRenderer.Unbaked INSTANCE = new Unbaked();
        @SuppressWarnings({"unchecked", "rawtypes"})
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public SpecialModelRenderer<Void> bake(SpecialModelRenderer.BakingContext context) {
            return new CreeperPlushieSpecialRenderer(new CreeperPlushieModel(PlushieGeometry.bakeRoot()));
        }

        @Override
        public MapCodec<? extends SpecialModelRenderer.Unbaked<Void>> type() {
            return MAP_CODEC;
        }
    }
}
