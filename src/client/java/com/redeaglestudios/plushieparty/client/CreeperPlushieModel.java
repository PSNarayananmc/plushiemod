package com.redeaglestudios.plushieparty.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
/**
 * The plushie entity model (geometry.re_plushie_party.creeper_plushie, 32x32).
 * Bedrock material entity_alphatest -> entityCutoutNoCull so the floating face
 * decals are visible from every angle.
 */
public class CreeperPlushieModel extends EntityModel<CreeperPlushieRenderState> {
    private static final Identifier TEXTURE = PlushiePartyClient.id("textures/entity/creeper_plushie.png");

    public final ModelPart body;

    public CreeperPlushieModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.body = root.getChild("body");
    }

    public static Identifier texture() {
        return TEXTURE;
    }

    @Override
    public void setupAnim(CreeperPlushieRenderState state) {
        super.setupAnim(state);
        float[] scale = null;
        if (state.squeezeStartAge >= 0f) {
            float seconds = (state.ageInTicks - state.squeezeStartAge) / 20f;
            if (seconds >= 0f && seconds <= 0.5f) {
                scale = PlushieAnimations.squeezeScale(seconds);
            }
        }
        if (scale != null) {
            this.body.xScale = scale[0];
            this.body.yScale = scale[1];
            this.body.zScale = scale[2];
        } else {
            this.body.xScale = 1f;
            this.body.yScale = 1f;
            this.body.zScale = 1f;
        }
    }
}
