package com.redeaglestudios.plushieparty;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * All mod registrations for Plushie Party.
 *
 * Faithful port of the Bedrock add-on:
 * - entity   re_plushie_party:creeper_plushie        -> plushie_party:creeper_plushie
 * - block    re_plushie_party:creeper_plushie_hitbox -> plushie_party:creeper_plushie (hitbox block)
 * - sounds   re_plushie_party:toy_use (3 random variants, vol 0.8, range 7)
 * - new:     plushie_party:gangnam_style music disc + its song
 */
public final class ModRegistry {
    private ModRegistry() {}

    // ------------------------------------------------------------------ ids
    public static final Identifier CREEPER_PLUSHIE_ID = PlushieParty.id("creeper_plushie");

    // ---------------------------------------------------------------- sounds
    /** Squeaky toy sounds (toy_use1/2/3, volume 0.8, max distance 7 like Bedrock). */
    public static final SoundEvent TOY_USE = SoundEvent.createFixedRangeEvent(PlushieParty.id("toy_use"), 7.0f);
    /** Replaceable silent party music slot (Bedrock ships a silent party_music.ogg too). */
    public static final SoundEvent PARTY_MUSIC = SoundEvent.createVariableRangeEvent(PlushieParty.id("party_music"));
    /** The Gangnam Style disc track (original synthesized party track, see README). */
    public static final SoundEvent GANGNAM_STYLE = SoundEvent.createVariableRangeEvent(PlushieParty.id("gangnam_style"));

    // ------------------------------------------------------------ jukebox song
    public static final ResourceKey<JukeboxSong> GANGNAM_STYLE_SONG_KEY =
        ResourceKey.create(Registries.JUKEBOX_SONG, PlushieParty.id("gangnam_style"));
    /** Track length in seconds (87.27s synthesized track). */
    public static final float GANGNAM_STYLE_LENGTH_SECONDS = 87.27f;

    // ----------------------------------------------------------------- entity
    public static final EntityType<CreeperPlushieEntity> CREEPER_PLUSHIE =
        Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CREEPER_PLUSHIE_ID,
            EntityType.Builder.<CreeperPlushieEntity>of(CreeperPlushieEntity::new, MobCategory.MISC)
                .sized(0.001f, 0.001f)          // Bedrock collision box 0x0
                .eyeHeight(0.0f)
                .clientTrackingRange(10)
                .updateInterval(20)
                .fireImmune()                    // minecraft:fire_immune
                .noLootTable()
                .build(ResourceKey.create(Registries.ENTITY_TYPE, CREEPER_PLUSHIE_ID))
        );

    // ------------------------------------------------------------------ block
    public static final ResourceKey<Block> CREEPER_PLUSHIE_BLOCK_KEY =
        ResourceKey.create(Registries.BLOCK, CREEPER_PLUSHIE_ID);

    /** Invisible hitbox block (Bedrock: re_plushie_party:creeper_plushie_hitbox). */
    public static final Block CREEPER_PLUSHIE_BLOCK = Registry.register(
        BuiltInRegistries.BLOCK,
        CREEPER_PLUSHIE_ID,
        new CreeperPlushieBlock(BlockBehaviour.Properties.of()
            .setId(CREEPER_PLUSHIE_BLOCK_KEY)
            .mapColor(MapColor.SNOW)                       // Bedrock map color #ffffff
            .strength(0.5f, 1200.0f)                       // 0.5s to mine, explosion-resistant
            .pushReaction(PushReaction.BLOCK)              // minecraft:movable immovable
            .noOcclusion()
            .sound(net.minecraft.world.level.block.SoundType.WOOL))
    );

    // ------------------------------------------------------------------ items
    public static final ResourceKey<Item> CREEPER_PLUSHIE_ITEM_KEY =
        ResourceKey.create(Registries.ITEM, CREEPER_PLUSHIE_ID);
    public static final ResourceKey<Item> GANGNAM_STYLE_DISC_KEY =
        ResourceKey.create(Registries.ITEM, PlushieParty.id("gangnam_style"));

    /** Block item - placing this spawns the plushie entity, exactly like Bedrock. */
    public static final Item CREEPER_PLUSHIE_ITEM = Registry.register(
        BuiltInRegistries.ITEM,
        CREEPER_PLUSHIE_ITEM_KEY,
        new BlockItem(CREEPER_PLUSHIE_BLOCK,
            new Item.Properties().stacksTo(1).useBlockDescriptionPrefix().setId(CREEPER_PLUSHIE_ITEM_KEY))
    );

    /** The Gangnam Style music disc (jukebox playable via datapack song). */
    public static final Item GANGNAM_STYLE_DISC = Registry.register(
        BuiltInRegistries.ITEM,
        GANGNAM_STYLE_DISC_KEY,
        new Item(new Item.Properties()
            .stacksTo(1)
            .jukeboxPlayable(GANGNAM_STYLE_SONG_KEY)
            .rarity(net.minecraft.world.item.Rarity.RARE)
            .setId(GANGNAM_STYLE_DISC_KEY))
    );

    // ------------------------------------------------------------------ init
    public static void init() {
        Registry.register(BuiltInRegistries.SOUND_EVENT, PlushieParty.id("toy_use"), TOY_USE);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PlushieParty.id("party_music"), PARTY_MUSIC);
        Registry.register(BuiltInRegistries.SOUND_EVENT, PlushieParty.id("gangnam_style"), GANGNAM_STYLE);

        PlushieParty.LOGGER.info("Plushie Party registries loaded");
    }
}
