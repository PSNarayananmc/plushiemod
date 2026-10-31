package com.redeaglestudios.plushieparty.client;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Render state for the plushie carrying the squeeze animation timing. */
public class CreeperPlushieRenderState extends LivingEntityRenderState {
    /** ageInTicks at which the current squeeze started; -1 = not squeezing. */
    public float squeezeStartAge = -1.0f;
}
