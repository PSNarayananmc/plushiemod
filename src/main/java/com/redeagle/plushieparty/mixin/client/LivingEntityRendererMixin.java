package com.redeagle.plushieparty.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.redeagle.plushieparty.PartyDanceCurves;
import com.redeagle.plushieparty.client.DanceState;
import com.redeagle.plushieparty.client.PlushiePartyClient;
import net.fabricmc.fabric.api.client.rendering.v1.FabricRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes every living entity (mobs and players) play the party dance while it
 * is inside a party radius, replacing Bedrock's
 * {@code guest.playAnimation("animation.re_furn.party_dance[_player]", ...)}.
 *
 * <p>The pose is applied at the tail of {@code setupRotations}, i.e. after
 * the yaw rotation and before the {@code scale(-1,-1,1)} flip - exactly the
 * "pre-flip" space in which the Bedrock bone transform maps over 1:1.
 * Vanilla wraps the whole model submit in a push/pop pair, so no cleanup is
 * needed. The player's own camera is untouched: the dance pose is skipped in
 * first person, matching the {@code v.is_first_person} guard in the original
 * {@code party_dance_player} animation.</p>
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Inject(
			method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL")
	)
	private void plushieparty$extractDance(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		DanceState.Entry entry = DanceState.get(entity.getId());
		((FabricRenderState) state).setData(PlushiePartyClient.DANCE_STATE_KEY, entry);
	}

	@Inject(
			method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
			at = @At("TAIL")
	)
	private void plushieparty$applyPartyDance(LivingEntityRenderState state, PoseStack poseStack, float bodyRot, float entityScale, CallbackInfo ci) {
		DanceState.Entry entry = ((FabricRenderState) state).getData(PlushiePartyClient.DANCE_STATE_KEY);
		if (entry == null) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		// v.is_first_person guard: never tilt the player's own first-person camera.
		if (minecraft.player != null && minecraft.player.getId() == entry.entityId()
				&& minecraft.options.getCameraType().isFirstPerson()) {
			return;
		}
		double now = DanceState.nowTicks() + (state.ageInTicks % 1.0F);
		double blend = PartyDanceCurves.blendFor((long) now, entry.startTicks(), entry.stopTicks());
		if (blend <= 0.0D) {
			return;
		}
		double seconds = (now - entry.startTicks()) / 20.0D;
		PartyDanceCurves.applyPreFlip(poseStack, PartyDanceCurves.evaluate(seconds, blend));
	}
}
