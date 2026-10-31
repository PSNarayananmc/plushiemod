# Plushie Party — Minecraft Java Edition 26.2 (Fabric)

A faithful Java Edition port of the Minecraft **Bedrock** add-on
[Plushie Party by Red Eagle Studios](https://www.curseforge.com/minecraft-bedrock/addons/plushie-party)
(built for private use, with permission of the original behavior/asset design).

![Minecraft 26.2](https://img.shields.io/badge/Minecraft-26.2-green) ![Fabric](https://img.shields.io/badge/Fabric-0.20.1%2B-blue) ![Java](https://img.shields.io/badge/Java-25-orange)

## What it does

**One toy: the Creeper Plushie.** Craft it, place it, squeeze it, throw a party.

- 🧸 **Creeper Plushie** — a custom-modeled doll (14 cubes, floating face decals,
  splayed legs), placed through an invisible hitbox block that keeps the doll
  entity in sync (no duplicates, no orphans, survives reloads).
- 🤏 **Squeeze toy** — click it: a 0.5 s squash-and-rebound animation (all 13
  Bedrock keyframes ported 1:1) plus one of **3 squeaky sounds**
  (volume 0.8, max hearing distance 7, exactly like Bedrock).
  It also squeezes once when placed.
- 🎉 **Party mode** — right-click with a **stick or any music disc**:
  - the plushie dances (bounce + squash + 360° spin + hop, the exact
    3.7 s / 8-beat Molang curves ported to Java),
  - **every player and mob within 16 blocks starts dancing** (whole-body
    bounce/spin/squash via the shared renderer, first person untouched),
  - **disc music loops until the party stops**,
  - right-click again (anything in hand) to stop; 0.25 s blend-out.
  - Mob AI is untouched — hostile mobs can still attack mid-party!
- 💿 **Gangnam Style music disc** — a built-in custom disc (see below).
- ⚙️ Server-authoritative party logic with the original **74-tick sync**,
  multiple simultaneous parties that never stop each other, and the original
  crafting recipe: **7 lime wool + 1 gunpowder**.

## The Gangnam Style disc

The mod ships its own **"Gangnam Style" music disc** so you can trigger and score
the party with one item. Because the real PSY recording is copyrighted, the disc
ships with an **original synthesized party track** (87 s, mono, energetic K-pop/EDM
style — composed in code, `scripts/gen_assets.py` in the build workspace).

**Want the real song?** Swap the audio (keeping the file name):

```
assets/plushie_party/sounds/disc/gangnam_style.ogg
```

1. Unzip `plushie_party-26.2.jar` (it is a zip) — or better, override it from a
   resource pack with the same path.
2. Convert your legally obtained copy to **OGG Vorbis, mono**
   (`ffmpeg -i song.mp3 -ac 1 song.ogg`).
3. Replace the file, update `length_in_seconds` in
   `data/plushie_party/jukebox_song/gangnam_style.json` if the duration differs,
   and zip it back.

The stick trigger plays the `plushie_party:party_music` sound slot — shipped
silent, resource-pack replaceable, exactly like the Bedrock pack's
`party_music.ogg` trick.

## Install

- **Minecraft Java 26.2** + **Fabric Loader 0.19.5 or newer** (tested on 0.20.1)
- Fabric API **0.161.0+26.2** (or newer for 26.2)
- Drop `plushie_party-26.2.jar` into `.minecraft/mods/`

## Building from source

```bash
./gradlew build            # requires Java 25 (Gradle wrapper 9.8.2)
# -> build/libs/plushie_party-26.2.jar
```

## Project layout

```
src/main/java/com/redeaglestudios/plushieparty/   server + shared code
  PlushieParty.java        mod entrypoint
  ModRegistry.java         entity/block/item/sound registration
  CreeperPlushieEntity.java the doll (no AI, invulnerable, orphan cleanup, squeeze)
  CreeperPlushieBlock.java  invisible hitbox block (placement/breaking/interaction)
  PartyManager.java         74-tick dancer sync, 16-block radius, music looping
  ModPayloads.java          clientbound dance start/stop packets
src/client/java/.../client/      rendering
  PlushieGeometry.java      Bedrock->Java mesh converter (per-face UVs)
  CreeperPlushieModel.java  model + squeeze animation
  CreeperPlushieRenderer.java
  PlushieAnimations.java    party dance math + squeeze keyframes
  DanceManager.java         client dance state from server sync
  CreeperPlushieSpecialRenderer.java  3D item form
  mixin/LivingEntityRendererMixin.java  makes all mobs & players dance
```

## Faithfulness notes & known limitations

- The Bedrock `re_furn:plushies_*` furniture hooks referenced by the scripts do
  not exist in this add-on's files, so they are not implemented; the party
  system stays modular like the original.
- The Bedrock idle animation references bones (`ball`, `fish`, `chew_bone`,
  `mouse`) that don't exist in the model — safely ignored, same as Bedrock does.
- The Bedrock **remote** and **guidebook** items described on the CurseForge
  page are part of *newer* add-on versions than the `.mcaddon` this port was
  built from; they are not included here.
- Leg UV mirroring uses negative-width UV rects converted per-face; orientation
  was verified offline against the decompiled vanilla cube conventions.
- The doll entity is a real custom entity (not a reskinned creeper) with zero
  collision, exactly like Bedrock's 0x0 collision box.

## Credits

- Original Bedrock add-on: **Red Eagle Studios** (CurseForge)
- Java port: built with Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Loom 1.19
