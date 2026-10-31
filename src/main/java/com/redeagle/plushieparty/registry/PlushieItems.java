package com.redeagle.plushieparty.registry;

import com.redeagle.plushieparty.PlushiePartyMod;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.JukeboxSong;

/**
 * Item registration.
 *
 * <p>{@code re_plushie_party:gangnam_style} - the "Gangnam Style" music disc
 * requested for the Java port. The song itself is data-driven
 * ({@code data/re_plushie_party/jukebox_song/gangnam_style.json}) and points
 * at the bundled original synthesized track, so the disc also works in a
 * vanilla jukebox with a working comparator output. Right-clicking a plushie
 * with it starts a party that loops the song (see {@code PartyManager}).</p>
 */
public final class PlushieItems {
        public static final Identifier GANGNAM_STYLE_ID = PlushiePartyMod.id("gangnam_style");
        public static final ResourceKey<Item> GANGNAM_STYLE_KEY = ResourceKey.create(BuiltInRegistries.ITEM.key(), GANGNAM_STYLE_ID);
        public static final ResourceKey<JukeboxSong> GANGNAM_STYLE_SONG_KEY = ResourceKey.create(Registries.JUKEBOX_SONG, GANGNAM_STYLE_ID);

        public static final Item GANGNAM_STYLE = Registry.register(
                        BuiltInRegistries.ITEM,
                        GANGNAM_STYLE_KEY,
                        new Item(new Item.Properties()
                                        .setId(GANGNAM_STYLE_KEY)
                                        .stacksTo(1)
                                        .jukeboxPlayable(GANGNAM_STYLE_SONG_KEY)));

        static {
                CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.BUILDING_BLOCKS)
                                .register(output -> output.accept(GANGNAM_STYLE));
        }

        public static void init() {
                // Class loading triggers registration.
        }

        private PlushieItems() {
        }
}
