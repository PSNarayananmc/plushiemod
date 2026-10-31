package com.redeagle.plushieparty.registry;

import com.redeagle.plushieparty.PlushiePartyMod;
import com.redeagle.plushieparty.entity.CreeperPlushieEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity registration.
 *
 * <p>{@code re_plushie_party:creeper_plushie} - a persistent, summonable,
 * zero-size, invulnerable decorative entity with no AI, exactly like the
 * Bedrock entity definition.</p>
 */
public final class PlushieEntities {
	public static final Identifier CREEPER_PLUSHIE_ID = PlushiePartyMod.id("creeper_plushie");
	public static final ResourceKey<EntityType<?>> CREEPER_PLUSHIE_KEY =
			ResourceKey.create(BuiltInRegistries.ENTITY_TYPE.key(), CREEPER_PLUSHIE_ID);

	public static final EntityType<CreeperPlushieEntity> CREEPER_PLUSHIE = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			CREEPER_PLUSHIE_KEY,
			EntityType.Builder
					.<CreeperPlushieEntity>of(CreeperPlushieEntity::new, MobCategory.MISC)
					.sized(0.0F, 0.0F) // Bedrock collision_box 0x0
					.clientTrackingRange(10)
					.updateInterval(20)
					.fireImmune() // Bedrock minecraft:fire_immune
					.build(CREEPER_PLUSHIE_KEY)
	);

	static {
		// Registration happens during class load.
	}

	public static void init() {
		// Class loading triggers registration.
	}

	private PlushieEntities() {
	}
}
