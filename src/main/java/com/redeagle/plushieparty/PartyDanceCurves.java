package com.redeagle.plushieparty;

import net.minecraft.util.Mth;

/**
 * Exact Java port of the Bedrock party dance animations from
 * {@code animations/plushie_party.animation.json}:
 *
 * <ul>
 *   <li>{@code animation.re_furn.party_dance} (plushies and mobs)</li>
 *   <li>{@code animation.re_furn.party_dance_player} (players; the first
 *       person guard is applied by the callers)</li>
 *   <li>{@code animation.re_furn.party_stop} (a 0.05 s reset, represented
 *       here together with the 0.25 s {@code blendOutTime} as a linear blend
 *       back to rest)</li>
 * </ul>
 *
 * <p>All Molang expressions were transcribed verbatim. The beat variable
 * {@code t.s = q.anim_time / 0.4625} yields 8 beats over the 3.7 s loop:
 * 4 bounce beats, then 2 beats with a 360&deg; spin, then 2 beats of hip
 * sway. Positions use Bedrock model units (1/16 block, +y up).</p>
 */
public final class PartyDanceCurves {
	/** Seconds per beat ({@code 0.4625} in the Molang expressions). */
	public static final double SECONDS_PER_BEAT = 0.4625D;
	/** 8 beats per loop, 3.7 seconds. */
	public static final double LOOP_SECONDS = SECONDS_PER_BEAT * 8.0D;
	/** {@code blendOutTime: 0.25} used by both dance and stop animations. */
	public static final double BLEND_SECONDS = 0.25D;

	/** One evaluated frame of the dance pose. Angles in degrees, offsetY in blocks (up), scales linear. */
	public record Pose(float rotationX, float rotationY, float rotationZ, float offsetY,
					   float scaleX, float scaleY, float scaleZ) {
		public static final Pose REST = new Pose(0, 0, 0, 0, 1, 1, 1);
	}

	private PartyDanceCurves() {
	}

	/**
	 * Evaluates the dance pose.
	 *
	 * @param seconds seconds since the dance started (looping)
	 * @param blend   0 = fully at rest, 1 = fully dancing (the Bedrock 0.25 s
	 *                blend-in/out window as a linear crossfade)
	 */
	public static Pose evaluate(double seconds, double blend) {
		blend = Mth.clamp(blend, 0.0D, 1.0D);
		if (blend <= 0.0D) {
			return Pose.REST;
		}
		double s = (seconds / SECONDS_PER_BEAT) % 8.0D;
		// t.w = math.sin(t.s * 360)  (Molang sin uses degrees)
		double w = Math.sin(Math.toRadians(s * 360.0D));

		// rotation: t.s < 4 ? 24 * math.pow(math.sin(t.s * 180), 2) : 0
		double rotX = s < 4.0D ? 24.0D * Math.pow(Math.sin(Math.toRadians(s * 180.0D)), 2.0D) : 0.0D;
		// rotation: t.p = clamp((t.s - 4) / 2, 0, 1); t.s < 6 ? 360 * t.p^2 * (3 - 2 t.p) : 0
		double p = Mth.clamp((s - 4.0D) / 2.0D, 0.0D, 1.0D);
		double rotY = s < 6.0D ? 360.0D * p * p * (3.0D - 2.0D * p) : 0.0D;
		// rotation: t.s < 4 ? 6*sin(t.s*90) : (t.s < 6 ? 0 : 14*sin((t.s-6)*180))
		double rotZ;
		if (s < 4.0D) {
			rotZ = 6.0D * Math.sin(Math.toRadians(s * 90.0D));
		} else if (s < 6.0D) {
			rotZ = 0.0D;
		} else {
			rotZ = 14.0D * Math.sin(Math.toRadians((s - 6.0D) * 180.0D));
		}
		// position: t.a = t.s < 4 ? 1 : (t.s < 6 ? 3.5 : 1.5); t.a * max(0, -t.w)
		double amplitude = s < 4.0D ? 1.0D : (s < 6.0D ? 3.5D : 1.5D);
		double posY = amplitude * Math.max(0.0D, -w) / 16.0D; // Bedrock model units -> blocks

		// scale: w > 0 ? 1 + 0.45 w : 1 + 0.2 w  (x/z)
		double scaleXZ = w > 0.0D ? 1.0D + 0.45D * w : 1.0D + 0.2D * w;
		// scale: w > 0 ? 1 - 0.55 w : 1 - 0.35 w  (y)
		double scaleY = w > 0.0D ? 1.0D - 0.55D * w : 1.0D - 0.35D * w;

		return new Pose(
				(float) (rotX * blend),
				(float) (rotY * blend),
				(float) (rotZ * blend),
				(float) (posY * blend),
				(float) Mth.lerp(blend, 1.0D, scaleXZ),
				(float) Mth.lerp(blend, 1.0D, scaleY),
				(float) Mth.lerp(blend, 1.0D, scaleXZ));
	}

	/** @return the 0..1 blend factor for a dance that started at {@code startTicks} and stopped at {@code stopTicks} (-1 = still dancing). */
	public static double blendFor(long nowTicks, long startTicks, long stopTicks) {
		double sinceStart = (nowTicks - startTicks) / 20.0D;
		double blendIn = Math.min(1.0D, sinceStart / BLEND_SECONDS);
		if (stopTicks >= 0) {
			double sinceStop = (nowTicks - stopTicks) / 20.0D;
			double blendOut = 1.0D - Math.min(1.0D, sinceStop / BLEND_SECONDS);
			return Math.min(blendIn, blendOut);
		}
		return blendIn;
	}

	/**
	 * Applies the dance pose to a pose stack in the "pre-flip" position used
	 * by vanilla entity rendering (after the yaw rotation, before the
	 * {@code scale(-1,-1,1)} flip). In this space the Bedrock bone transform
	 * maps over 1:1: +y is up, and rotations use the raw Bedrock angles
	 * composed in ZYX order (both engines' entity renderers apply the same
	 * 180&deg; yaw offset, so the flipped axes cancel out).
	 */
	public static void applyPreFlip(com.mojang.blaze3d.vertex.PoseStack poseStack, Pose pose) {
		if (pose.offsetY() != 0.0F) {
			poseStack.translate(0.0D, pose.offsetY(), 0.0D);
		}
		if (pose.rotationX() != 0.0F || pose.rotationY() != 0.0F || pose.rotationZ() != 0.0F) {
			poseStack.mulPose(new org.joml.Quaternionf().rotationZYX(
					(float) Math.toRadians(pose.rotationZ()),
					(float) Math.toRadians(pose.rotationY()),
					(float) Math.toRadians(pose.rotationX())));
		}
		if (pose.scaleX() != 1.0F || pose.scaleY() != 1.0F || pose.scaleZ() != 1.0F) {
			poseStack.scale(pose.scaleX(), pose.scaleY(), pose.scaleZ());
		}
	}
}
