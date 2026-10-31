package com.redeagle.plushieparty;

import com.redeagle.plushieparty.network.PlushieNetworking;
import com.redeagle.plushieparty.registry.PlushieBlocks;
import com.redeagle.plushieparty.registry.PlushieEntities;
import com.redeagle.plushieparty.registry.PlushieSounds;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plushie Party - a faithful Java Edition port of the "Plushie Party"
 * Minecraft Bedrock addon by RedEagleStudios.
 *
 * <p>Right-click a plushie with a stick or a music disc to start a party:
 * every player and mob within 16 blocks starts dancing.</p>
 */
public class PlushiePartyMod implements ModInitializer {
        public static Identifier id(String path) {
                return Identifier.fromNamespaceAndPath(NAMESPACE, path);
        }

        public static final String MOD_ID = "plushie_party";
        /** Original Bedrock resource namespace, kept for fidelity. */
        public static final String NAMESPACE = "re_plushie_party";
        public static final Logger LOGGER = LoggerFactory.getLogger("Plushie Party");

        @Override
        public void onInitialize() {
                PlushieSounds.init();
                PlushieEntities.init();
                PlushieBlocks.init();
                PlushieNetworking.init();
                LOGGER.info("Plushie Party initialized - let's dance!");
        }
}
