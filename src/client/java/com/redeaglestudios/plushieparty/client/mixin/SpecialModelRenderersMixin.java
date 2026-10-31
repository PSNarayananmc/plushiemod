package com.redeaglestudios.plushieparty.client.mixin;

import net.minecraft.client.renderer.special.SpecialModelRenderers;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.redeaglestudios.plushieparty.PlushieParty;
import com.redeaglestudios.plushieparty.client.CreeperPlushieSpecialRenderer;

/**
 * Registers the plushie's 3D item renderer as a "minecraft:special" type by
 * adding it to the vanilla SpecialModelRenderers id mapper during bootstrap.
 */
@Mixin(SpecialModelRenderers.class)
public abstract class SpecialModelRenderersMixin {

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void plushieParty$registerSpecialRenderer(CallbackInfo ci) {
        SpecialModelRenderersAccessor.plushieParty$idMapper().put(
            PlushieParty.id("creeper_plushie"),
            CreeperPlushieSpecialRenderer.Unbaked.MAP_CODEC
        );
    }
}
