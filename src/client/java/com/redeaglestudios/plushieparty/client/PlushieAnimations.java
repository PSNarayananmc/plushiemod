package com.redeaglestudios.plushieparty.client;

/**
 * Animation data ported 1:1 from the Bedrock add-on.
 *
 * 1) Squeeze (animation.re_plushie_party.plushie_click_squeak, 0.5s, body scale)
 * 2) Party dance (animation.re_furn.party_dance, 3.7s loop, root bone) - the
 *    Molang expressions were evaluated analytically:
 *      s    = anim_time / 0.4625            (8 beats of 0.4625s)
 *      w    = sin(s * 360deg)
 *      rotX = s < 4 ? 24 * sin(s*180deg)^2 : 0
 *      rotY = s < 6 ? 360 * smoothstep((s-4)/2)^2... : 0    (full 360deg spin, beats 4-6)
 *      rotZ = s < 4 ? 6 * sin(s*90deg) : (s < 6 ? 0 : 14 * sin((s-6)*180deg))
 *      posY = amp * max(0, -w), amp = s<4 ? 1 : (s<6 ? 3.5 : 1.5)
 *      scaleXZ = w > 0 ? 1 + 0.45w : 1 + 0.2w
 *      scaleY  = w > 0 ? 1 - 0.55w : 1 - 0.35w
 * 3) Party stop: transforms ease back to identity over 0.25s (blendOutTime).
 */
public final class PlushieAnimations {
    private PlushieAnimations() {}

    public static final float PARTY_LENGTH_SECONDS = 3.7f;
    public static final float BEAT_SECONDS = 0.4625f;   // 8 beats per loop
    public static final float STOP_BLEND_SECONDS = 0.25f;

    /** Evaluation result for one moment of the party dance. */
    public record DanceFrame(float rotXDeg, float rotYDeg, float rotZDeg,
                             float upUnits, float scaleX, float scaleY, float scaleZ) {}

    /** Evaluates the party dance at phase t (seconds since the dance started). */
    public static DanceFrame partyDance(float seconds) {
        float s = seconds / BEAT_SECONDS;
        // Keep the phase inside one loop: the animation loops seamlessly.
        s = s % 8f;
        if (s < 0) {
            s += 8f;
        }

        float w = (float) Math.sin(Math.toRadians(s * 360.0));

        float rotX = s < 4f ? 24f * (float) Math.pow(Math.sin(Math.toRadians(s * 180.0)), 2) : 0f;
        float rotY;
        if (s < 6f) {
            float p = Math.max(0f, Math.min(1f, (s - 4f) / 2f));
            rotY = 360f * p * p * (3f - 2f * p);
        } else {
            rotY = 0f;
        }
        float rotZ;
        if (s < 4f) {
            rotZ = 6f * (float) Math.sin(Math.toRadians(s * 90.0));
        } else if (s < 6f) {
            rotZ = 0f;
        } else {
            rotZ = 14f * (float) Math.sin(Math.toRadians((s - 6f) * 180.0));
        }

        float amp = s < 4f ? 1f : (s < 6f ? 3.5f : 1.5f);
        float up = amp * Math.max(0f, -w);

        float scaleXZ = w > 0f ? 1f + 0.45f * w : 1f + 0.2f * w;
        float scaleY = w > 0f ? 1f - 0.55f * w : 1f - 0.35f * w;

        return new DanceFrame(rotX, rotY, rotZ, up, scaleXZ, scaleY, scaleXZ);
    }

    /** Squeeze keyframes from plushie_click_squeak (seconds -> [sx, sy, sz]). */
    private static final float[][] SQUEEZE_KEYS = {
        {0.0f, 1f, 1f, 1f},
        {0.0417f, 1.0178f, 0.9131f, 1.0163f},
        {0.0833f, 1.05f, 0.8042f, 1.05f},
        {0.125f, 1.0822f, 0.7499f, 1.0837f},
        {0.1667f, 1.1f, 0.8f, 1.1f},
        {0.2083f, 1.0644f, 0.9534f, 1.0674f},
        {0.25f, 1f, 1.1254f, 1f},
        {0.2917f, 0.9356f, 1.2231f, 0.9326f},
        {0.3333f, 0.9f, 1.2f, 0.9f},
        {0.375f, 0.9178f, 1.1266f, 0.9163f},
        {0.4167f, 0.95f, 1.0663f, 0.95f},
        {0.4583f, 0.9822f, 1.0212f, 0.9837f},
        {0.5f, 1f, 1f, 1f},
    };

    /** Linear interpolation over the squeeze keyframes (Bedrock default easing). */
    public static float[] squeezeScale(float seconds) {
        if (seconds <= 0f) {
            float[] k = SQUEEZE_KEYS[0];
            return new float[] {k[1], k[2], k[3]};
        }
        for (int i = 1; i < SQUEEZE_KEYS.length; i++) {
            float[] prev = SQUEEZE_KEYS[i - 1];
            float[] next = SQUEEZE_KEYS[i];
            if (seconds <= next[0]) {
                float a = (seconds - prev[0]) / (next[0] - prev[0]);
                return new float[] {
                    prev[1] + (next[1] - prev[1]) * a,
                    prev[2] + (next[2] - prev[2]) * a,
                    prev[3] + (next[3] - prev[3]) * a,
                };
            }
        }
        return new float[] {1f, 1f, 1f};
    }
}
