package com.redeagle.plushieparty.registry;

import com.redeagle.plushieparty.PlushiePartyMod;
import com.redeagle.plushieparty.block.CreeperPlushieHitboxBlock;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

/**
 * Block and item registration.
 *
 * <p>{@code re_plushie_party:creeper_plushie_hitbox} mirrors the Bedrock
 * block: cloth sounds, white map colour, 0.5 s mining time, explosion proof,
 * immovable by pistons, no loot table surprises (drops itself), listed in the
 * building blocks tab (Bedrock menu_category "construction").</p>
 */
public final class PlushieBlocks {
        public static final Identifier HITBOX_ID = PlushiePartyMod.id("creeper_plushie_hitbox");
        public static final ResourceKey<Block> HITBOX_KEY = ResourceKey.create(BuiltInRegistries.BLOCK.key(), HITBOX_ID);
        public static final ResourceKey<Item> HITBOX_ITEM_KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), HITBOX_ID);

        public static final Block CREEPER_PLUSHIE_HITBOX = Registry.register(
                        BuiltInRegistries.BLOCK,
                        HITBOX_KEY,
                        new CreeperPlushieHitboxBlock(BlockBehaviour.Properties.of()
                                        .setId(HITBOX_KEY)
                                        .mapColor(MapColor.SNOW) // Bedrock minecraft:map_color #ffffff
                                        .strength(0.5F, 3600000.0F) // seconds_to_destroy 0.5, destructible_by_explosion false
                                        .sound(SoundType.WOOL) // Bedrock "sound": "cloth"
                                        .noOcclusion() // selection/collision boxes are custom
                                        .pushReaction(PushReaction.BLOCK))); // minecraft:movable immovable

        public static final Item CREEPER_PLUSHIE_HITBOX_ITEM = Registry.register(
                        BuiltInRegistries.ITEM,
                        HITBOX_ITEM_KEY,
                        new BlockItem(CREEPER_PLUSHIE_HITBOX, new Item.Properties()
                                        .setId(HITBOX_ITEM_KEY)
                                        .useBlockDescriptionPrefix()));

        static {
                CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                                .register(output -> output.accept(CREEPER_PLUSHIE_HITBOX_ITEM));
        }

        public static void init() {
                // Class loading triggers registration.
        }

        private PlushieBlocks() {
        }
}
