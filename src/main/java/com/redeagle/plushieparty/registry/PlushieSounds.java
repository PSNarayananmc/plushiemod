package com.redeagle.plushieparty.registry;

import com.redeagle.plushieparty.PlushiePartyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

/**
 * Sound events. Mirrors the Bedrock sound definition:
 * {@code re_plushie_party:toy_use} - one of three random {@code toy_use*.ogg}
 * samples, volume 0.8, pitch 1.0, maximum audible distance 7 blocks,
 * category neutral.
 */
public final class PlushieSounds {
	public static final Identifier TOY_USE_ID = PlushiePartyMod.id("toy_use");
	/** Fixed 7-block range, exactly like the Bedrock {@code max_distance: 7}. */
	public static final SoundEvent TOY_USE = SoundEvent.createFixedRangeEvent(TOY_USE_ID, 7.0F);

	static {
		Registry.register(BuiltInRegistries.SOUND_EVENT, TOY_USE_ID, TOY_USE);
	}

	public static void init() {
		// Class loading triggers registration.
	}

	private PlushieSounds() {
	}
}
