# Plushie Party — Java Edition Port

A faithful Minecraft: Java Edition **Fabric mod** port of the Minecraft Bedrock addon
[**Plushie Party** by RedEagleStudios](https://www.curseforge.com/minecraft-bedrock/addons/plushie-party).

Right-click the **Creeper Plushie** with a **stick** or any **music disc** and everyone
within 16 blocks — players, mobs and the plushie itself — starts dancing.
The port also bundles a custom **"Gangnam Style" music disc** (an original synthesized
party track — see [The Gangnam Style music disc](#the-gangnam-style-music-disc)).

| | |
|---|---|
| Target | Minecraft Java **26.2** (Fabric Loader **0.20.1**, Fabric API **0.161.0+26.2**) |
| Java | **25** (required by Minecraft 26.2) |
| Mod id | `plushie_party` |
| Registry namespace | `re_plushie_party` (identical to the Bedrock addon — commands match: `/summon re_plushie_party:creeper_plushie`) |
| Download | [`releases/plushie_party-26.2.jar`](releases/plushie_party-26.2.jar) (also on the [Releases page](https://github.com/PSNarayananmc/plushiemod/releases)) |
| Build | `./gradlew build` (or `gradle build`) → `build/libs/plushie_party-26.2.jar` |

---

## What is ported (1:1 from the original addon)

### Content
- **Creeper Plushie** — the only plushie in the original addon; nothing invented, nothing removed.
- **Invisible hitbox block** `re_plushie_party:creeper_plushie_hitbox`:
  - renders nothing in the world (the Bedrock block uses a 1/16-unit cube with a fully transparent texture)
  - 10/16³ collision + selection box centered in the block (`placed` blockstate), set on placement
  - 0.5 s mining time, **explosion-proof**, **immovable by pistons**, cloth sounds, white map color
  - break particles use the plushie texture (like Bedrock `destruction_particles`)
- **Crafting recipe** — exact Bedrock pattern: `###` / `#G#` / `# #` = 7× lime wool + 1× gunpowder → 1× Creeper Plushie.
- **Sounds** — the original `toy_use1/2/3.ogg` files, random pick, **volume 0.8, pitch 1.0,
  max distance 7 blocks** (`SoundEvent.createFixedRangeEvent(..., 7f)`), neutral category.
- **Creative tab** — item appears in Building Blocks (Bedrock `menu_category: construction`).

### Placement system
- Place the block item → the plushie **entity** spawns at the block center, rotated to
  **face the placing player** with the exact yaw table of the original script
  (`south→180, north→0, east→90, west→270`; Java `Direction#toYRot` of the facing
  produces the identical rotation).
- Breaking the block removes the entity (Bedrock `playerBreakBlock` handler).
- Orphan protection: the entity despawns if the block it sits in becomes air/water.
  *(The original script intended this via `inside_block_notifier`, but its despawn event
  added a non-existent component group and never fired; the port implements the intent,
  which also cleans up entities whose block was removed by commands.)*

### Click interaction
- **Every** right-click on the block squishes the plushie (Bedrock fires
  `playerInteractWithBlock` for any held item) and plays a random squeak sound.
- The squish is the original 13-keyframe, 0.5 s body-scale animation
  (`plushie_click_squeak`, linear interpolation, hold on last frame), applied to the
  `body` bone around its ground pivot.

### Party system (server-authoritative)
A 1:1 port of `scripts/main.js`:
- Stick or any `minecraft:music_disc_*` item in the **main hand** toggles the party
  (empty hand / other items never toggle — same as Bedrock).
- A sync pass runs immediately on party start and then every **74 ticks**
  (`system.runInterval(74)`).
- Each pass gathers all players and all mobs within **16 blocks** of the host plus the
  host itself; newcomers start dancing, dancers that left every party radius stop.
- **Multiple parties** merge naturally: a dancer only stops outside *all* party radii,
  and stopping one party never stops dancers still inside another.
- When the last party ends the ticker stops (`system.clearRun`).
- Removed hosts/dancers are pruned immediately and at every sync pass.

### Party music (Java-port addition)
The Bedrock addon leaves the party silent — the music in the demo video comes from
outside the addon. The Java port plays real music at the plushie instead:
- Start the party with **any music disc** (vanilla discs or the bundled Gangnam Style
  disc) → **that disc's song loops at the plushie** (category *Records*, volume 4 ≈
  64-block audible radius) until the party is toggled off or the host is removed —
  the sound is cut instantly via a stop-sound packet, not left to fade.
- Start it with a **stick** → the bundled `re_plushie_party:party_music` loop plays
  instead (see replacement guide below).
- The loop interval is the **real track length** (`JukeboxSong#lengthInTicks` for discs,
  the ogg header for the stick loop), so songs restart cleanly at the beat and
  user-replaced tracks of any length keep working.
- Multiple parties each keep their own music state, like the rest of the party system.

### Animations (exact Molang curve port)
The three party animations from `plushie_party.animation.json` were transcribed verbatim
into `PartyDanceCurves.java` (8 beats of 0.4625 s over the 3.7 s loop):
4 bounce beats (24° `sin²` bob, 6° sway, squash/stretch `1±0.45w`/`1∓0.55w`, hop up to
3.5/16 blocks), 2 beats of a smoothstepped 360° spin, 2 beats of 14° hip sway.
The 0.25 s `blendOutTime` crossfade and the `party_stop` reset are implemented as the
same linear blend. Players get the `party_dance_player` variant — **skipped in first
person** (`v.is_first_person` guard), so the camera is never affected.

### Rendering
- **Custom renderer + custom mesh** (no GeckoLib — it is not available for 26.2): all
  **14 cubes** of `geometry.re_plushie_party.creeper_plushie` were converted 1:1 to Java
  model space (y-flip, ground at 24) by `scripts/generate_model.py`, including the four
  splayed legs (−70°/70° X, ∓15° Y on their own pivots) and all per-face UVs with
  mirrored faces (negative UV sizes) preserved.
- **Item display** recreates the Bedrock `minecraft:item_visual` thumbnail (the same
  doll rotated 180°) with a custom `ItemModel` + `SpecialModelRenderer`, because the
  leg rotations (70°) cannot be expressed in vanilla block-model JSON.
- Mobs and players dance through a client mixin into `LivingEntityRenderer` (applied in
  the same pre-flip transform space the Bedrock bone animation runs in), driven by
  server dance packets that carry the server clock so every client renders the same beat.

---

## Differences & known limitations (honest list)

| Original (Bedrock) | Port (Java) |
|---|---|
| `inside_block_notifier` despawn (broken in the original — never fired) | Implemented as intended: entity despawns in air/water |
| `mark_variant0/1` component groups | Unused by the original script (vestigial) — not ported |
| `re_furn:plushies_*` block/entity handling | **Not ported** — those assets don't exist in this addon; `PartyManager.toggleParty` is host-agnostic, ready for a future Re-Furn port |
| Yarn/mojmap-era Fabric versions | Built for the unobfuscated 26.2 pipeline (no mappings, plain `implementation`) |
| Bedrock `entity_alphatest` material | `RenderTypes.entityCutout` (alpha test, double-sided) |
| Idle animation channels for `ball`, `fish`, `chew_bone`, `mouse` | Those bones **do not exist** in this model (they belong to other Re-Furn plushies); channels are safely ignored, exactly like Bedrock |
| GUI/client visuals | Verified by compilation against the exact 26.2 API + server-side runtime tests; a visual in-client pass (F5 over a placed plushie) is recommended since this sandbox has no display |

## Verified by runtime tests (dedicated server, headless)

- Mod initializes and the world loads with no errors (`Plushie Party initialized`)
- Block, entity, item, sound, recipe and loot table all load (1586 recipes parsed)
- `/summon re_plushie_party:creeper_plushie` works, entity named "Creeper Plushie"
- Plushie inside its hitbox block **persists**; plushie in open air **self-discards**
- `/give re_plushie_party:creeper_plushie_hitbox` works; recipe + loot table parse
- Gangnam Style disc, both music tracks and the `gangnam_style` jukebox song load;
  party music loop + stop verified against the exact 26.2 API (`JukeboxSong#fromStack`,
  `ClientboundStopSoundPacket`) and a clean 26.2 server boot with datapacks

## The Gangnam Style music disc

The requested custom disc is included:

| | |
|---|---|
| Item | `re_plushie_party:gangnam_style` — **"Gangnam Style"**, found in the Building Blocks creative tab next to the plushie |
| Works in jukeboxes | Yes — real `jukebox_playable` item, comparator output 1 (`data/re_plushie_party/jukebox_song/gangnam_style.json`) |
| Party | Right-click a plushie with it → party starts and the song loops at the plushie |
| Track | **87.27 s original synthesized K-pop/EDM-style party track** (132 BPM, Am–F–C–G hook) |

**About the audio:** the real PSY recording is copyrighted and is **not** included —
shipping it would be piracy. The bundled track is an original composition in the same
energetic party style, so the mod is safe to use and share.

### Replacing the disc track with the real song (or any song)

1. Get a legally obtained copy of the song you own and convert it to mono OGG Vorbis:
   `ffmpeg -i input.mp3 -ac 1 -c:a libvorbis -qscale:a 5 gangnam_style.ogg`
2. Replace `assets/re_plushie_party/sounds/gangnam_style.ogg` inside the jar (any zip
   tool) — or in the source tree, then rebuild.
3. Update `length_in_seconds` in `data/re_plushie_party/jukebox_song/gangnam_style.json`
   to the real song length (the in-jukebox behaviour and the party loop both follow it).

### Replacing the stick-party loop

`assets/re_plushie_party/sounds/party_music.ogg` is the track played when a party is
started with a stick (bundled: a 14.8 s original loop that is an exact multiple of the
74-tick dance cycle, so it stays on beat). Replace it the same way; the mod parses the
ogg length at runtime, so any duration loops correctly.

## Installing

1. Install the [Fabric Loader](https://fabricmc.net/use/) for Minecraft **26.2**.
2. Drop [Fabric API](https://modrinth.com/mod/fabric-api) `0.161.0+26.2` (or newer for 26.2) into `mods/`.
3. Drop `plushie_party-26.2.jar` into `mods/`.

## Building from source

```bash
# Requires JDK 25 (Minecraft 26.2 toolchain)
gradle build
# output: build/libs/plushie_party-26.2.jar
```

The model data is generated from the original Bedrock geometry:

```bash
python3 scripts/generate_model.py   # regenerates PlushieGeometry.java
```

## Credits

- **Plushie Party** Bedrock addon by **RedEagleStudios** (all original assets, geometry,
  animations and sounds belong to them).
- Java Edition port by PSNarayananmc (private use — not licensed for distribution).
