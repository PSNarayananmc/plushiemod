package com.redeagle.plushieparty.registry;

import com.redeagle.plushieparty.PlushiePartyMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;

import java.io.IOException;
import java.io.InputStream;

/**
 * Sound events. Mirrors the Bedrock sound definition:
 * {@code re_plushie_party:toy_use} - one of three random {@code toy_use*.ogg}
 * samples, volume 0.8, pitch 1.0, maximum audible distance 7 blocks,
 * category neutral.
 *
 * <p>Two music events are added for the party music feature (the Bedrock
 * original leaves the party silent and lets the video creator supply the
 * tune; the Java port loops real music at the plushie):</p>
 * <ul>
 *   <li>{@code re_plushie_party:gangnam_style} - the bundled disc track
 *       (an original synthesized composition, 87.27 s)</li>
 *   <li>{@code re_plushie_party:party_music} - the stick-party backing loop
 *       (original synthesized loop, exactly 32 dance beats = 14.8 s). Replace
 *       this ogg with any track you own; the loop interval is parsed from the
 *       file at runtime, so any duration keeps working.</li>
 * </ul>
 */
public final class PlushieSounds {
        public static final Identifier TOY_USE_ID = PlushiePartyMod.id("toy_use");
        /** Fixed 7-block range, exactly like the Bedrock {@code max_distance: 7}. */
        public static final SoundEvent TOY_USE = SoundEvent.createFixedRangeEvent(TOY_USE_ID, 7.0F);

        /** Party backing track played when a party is started with a stick. */
        public static final Identifier PARTY_MUSIC_ID = PlushiePartyMod.id("party_music");
        public static final SoundEvent PARTY_MUSIC = SoundEvent.createVariableRangeEvent(PARTY_MUSIC_ID);

        /** Track of the bundled "Gangnam Style" music disc. */
        public static final Identifier GANGNAM_STYLE_ID = PlushiePartyMod.id("gangnam_style");
        public static final SoundEvent GANGNAM_STYLE = SoundEvent.createVariableRangeEvent(GANGNAM_STYLE_ID);

        private static volatile Long partyMusicTicks;

        static {
                Registry.register(BuiltInRegistries.SOUND_EVENT, TOY_USE_ID, TOY_USE);
                Registry.register(BuiltInRegistries.SOUND_EVENT, PARTY_MUSIC_ID, PARTY_MUSIC);
                Registry.register(BuiltInRegistries.SOUND_EVENT, GANGNAM_STYLE_ID, GANGNAM_STYLE);
        }

        public static void init() {
                // Class loading triggers registration.
        }

        /**
         * Length of {@code party_music.ogg} in ticks, parsed from the bundled
         * ogg (last Ogg page granule position / sample rate), plus a 2-tick
         * gap so consecutive loops never overlap. Falls back to 74 ticks
         * (one dance cycle) if the file cannot be read.
         */
        public static long partyMusicLengthTicks() {
                Long cached = partyMusicTicks;
                if (cached != null) {
                        return cached;
                }
                long ticks = 74L;
                try (InputStream in = PlushieSounds.class.getResourceAsStream(
                                "/assets/re_plushie_party/sounds/party_music.ogg")) {
                        if (in != null) {
                                byte[] data = in.readAllBytes();
                                long granule = readLongLE(data, findLast(data, "OggS") + 6);
                                long rate = readUIntLE(data, find(data, new byte[]{1, 'v', 'o', 'r', 'b', 'i', 's'}) + 12);
                                if (granule > 0 && rate > 0) {
                                        ticks = (long) Math.ceil(granule / (double) rate * 20.0) + 2L;
                                }
                        }
                } catch (IOException | ArrayIndexOutOfBoundsException ignored) {
                        // keep the 74-tick fallback
                }
                partyMusicTicks = ticks;
                return ticks;
        }

        private static int find(byte[] data, byte[] pattern) {
                outer:
                for (int i = 0; i <= data.length - pattern.length; i++) {
                        for (int j = 0; j < pattern.length; j++) {
                                if (data[i + j] != pattern[j]) {
                                        continue outer;
                                }
                        }
                        return i;
                }
                return -1;
        }

        private static int findLast(byte[] data, String ascii) {
                byte[] pattern = ascii.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
                for (int i = data.length - pattern.length; i >= 0; i--) {
                        for (int j = 0; j < pattern.length; j++) {
                                if (data[i + j] != pattern[j]) {
                                        break;
                                }
                                if (j == pattern.length - 1) {
                                        return i;
                                }
                        }
                }
                return -1;
        }

        private static long readLongLE(byte[] data, int offset) {
                long value = 0;
                for (int i = 7; i >= 0; i--) {
                        value = (value << 8) | (data[offset + i] & 0xFFL);
                }
                return value;
        }

        private static long readUIntLE(byte[] data, int offset) {
                long value = 0;
                for (int i = 3; i >= 0; i--) {
                        value = (value << 8) | (data[offset + i] & 0xFFL);
                }
                return value;
        }

        private PlushieSounds() {
        }
}
