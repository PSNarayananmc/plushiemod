package com.redeaglestudios.plushieparty;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plushie Party - Java Edition port of the Bedrock add-on by Red Eagle Studios.
 *
 * A squeezable Creeper Plushie that throws dance parties: every player and mob
 * within 16 blocks starts dancing, and a music disc (like the built-in
 * Gangnam Style disc) keeps the song looping until the party stops.
 */
public class PlushieParty implements ModInitializer {
        public static final String MOD_ID = "plushie_party";

        // This logger is used to write text to the console and the log file.
        // It is considered best practice to use your mod id as the logger's name.
        // That way, it's clear which mod wrote info, warnings and errors.
        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        @Override
        public void onInitialize() {
                ModRegistry.init();
                ModPayloads.register();
                PartyManager.init();

                // Entity attributes (required before the doll can spawn).
                net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                        ModRegistry.CREEPER_PLUSHIE, CreeperPlushieEntity.createAttributes());

                // Creative tabs: the plushie with the building blocks, the disc with the tools.
                net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(
                        net.minecraft.world.item.CreativeModeTabs.BUILDING_BLOCKS).register(
                                entries -> entries.accept(ModRegistry.CREEPER_PLUSHIE_ITEM));
                net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents.modifyOutputEvent(
                        net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES).register(
                                entries -> entries.accept(ModRegistry.GANGNAM_STYLE_DISC));

                LOGGER.info("Plushie Party initialized - time to dance!");
        }

        public static net.minecraft.resources.Identifier id(String path) {
                return net.minecraft.resources.Identifier.fromNamespaceAndPath(MOD_ID, path);
        }
}
