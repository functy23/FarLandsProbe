# FarLands Probe (farlandsprobe)

**[English](README.md) | [简体中文](README.zh-CN.md)**

A Minecraft **26.2 / 26.1.2** Fabric mod (deobfuscated / Mojang official mappings) for exploring what happens past the world edge, where precision starts to break down. The same source tree builds for both game versions.

> ⚠️ **Note: this mod does NOT restore the Far Lands.** The classic "Far Lands" is a Beta-1.8-era noise-wall terrain bug at ~12,550,824 blocks. This mod does the opposite: it removes the modern world border and coordinate limits so you can push past them and observe precision corruption (lighting corruption, chunk structures overlapping, rendering vanishing, ...). Want the authentic Far Lands terrain? Look for a real Far Lands restoration mod.

All features are enabled by default, but **every feature can be toggled independently**.

## Changelog

**v1.2.1** — Maintenance pass (no behavior change):
- All Java comments unified to Chinese (previously mixed EN/ZH).
- Deduplicated the 2^28 wrap period: single source in `SectionEncoding#WRAP_PERIOD` (was defined twice, in `WorldGenRegionMixin` and `StaticCache2DMixin`).
- Flattened `WorldGenRegion#wrapChunkRequest`: static offset array + extracted `lookupWrappedChunk` helper, loop nesting down to 3 levels.
- `FarLandsProbeConfig`: every toggle now documents its feature → mixin mapping, so the README is no longer the only index of what each option controls.

**v1.2.0** — Multi-version support:
- Added a **Minecraft 26.1.2** build. Every mixin target is API-identical between 26.1.2 and 26.2, so both versions share the exact same sources with zero differences.
- The build is now a twin-project setup (root = 26.2, `mc-26.1.2` = 26.1.2); `./gradlew build` produces both jars in one run.

**v1.1.2** — Restored the far-edge stability patches and cleaned up the codebase:
- Re-added edge-chunk worldgen guards (`WorldGenRegion`/`StaticCache2D` wrap handling, edge feature-skip, pathfinding-skip, aquifer int-edge guard) so generating near/past the int32 edge **no longer OOMs or freezes** — but **no coordinate wrap-around**: past ±2,147,483,647 terrain simply stops generating and the game stays responsive.
- Mineshafts generate again (removed the global disable datapack; the overflow-safe midpoint patch covers far coordinates).
- HMAP-driven cleanup: fullbright centralized, unit tests added (11 passing), magic numbers documented, `.editorconfig` added, support classes moved out of the mixin package (fixed a worldgen crash).

**v1.1.1** — Removed all code that tried to generate chunks beyond the 32-bit limit, and reverted the Y-axis experiment:
- Section encoding is back to **X/Z 28 bits + Y 8 bits** (X/Z reach the int32 edge ±2,147,483,632 blocks; Y stays at the world-height range)
- Removed coordinate wrap-around, edge-chunk worldgen hacks (WorldGenRegion/StaticCache2D), edge feature-skip and pathfinding-skip
- **±2,147,483,647 is the hard end**: past it coordinates wrap in `int` and terrain does not generate — that is the physical limit
- C2ME auto-compat retained (falls back to vanilla encoding when C2ME is detected)

## Configuration

- **Config file**: `config/farlandsprobe.json` (auto-generated on first launch)
- **Config screen**: provided by **Cloth Config** (**required**), grouped into five categories:
  Lighting / World border / Spawn & teleport bounds / Coordinate encoding / Far-lands stability
- **Mod Menu (optional)**: Mods → FarLandsProbe → Config
- ⚠️ **Changing settings requires a game restart** — every option carries the "restart" badge

## The 32-bit limit

> `BlockPos` stores coordinates in a signed 32-bit `int`, so **±2,147,483,647 is the physical ceiling**. The extended section encoding pushes the render/generation limit right up to that edge, and the edge guards keep generation stable there. **Past it, coordinates wrap in `int` and terrain does not generate** — the world does not fold; it simply stops. Going further would require arbitrary-precision coordinates (a whole game fork, e.g. MCBig-style), which is not possible in a mod.

## C²M (C2ME) compatibility

> ⚠️ **C²M Engine rewrites the chunk storage/async loading system and is incompatible with the extended 28/8/28 section packing.** When C2ME is detected, the mod **automatically falls back to vanilla section encoding** (log: `C²M Engine detected: 28/8/28 section encoding auto-disabled`). In a C2ME profile you can still remove the border / movement clamps / teleport bounds, but far-lands deep exploration (beyond ±33,554,432 blocks) will not generate. Use a **non-C2ME environment** for that.

## Rendering caveats

> ⚠️ Near the ±2,147,483,647 edge, rendering can glitch: **chunk flashing / flickering, sections popping in and out, light flicker, blocks briefly disappearing** (the render octree and section encoding are working right at the int limit). This is expected — the mod is deliberately exploring the breakdown, not polishing it.

## Toggle overview

| Setting | Default | Description |
|---|---|---|
| Lighting → Fullbright (max light everywhere) | On | No darkness anywhere |
| World border → Remove world border | On | Border wall / damage / red vignette / clamping removed |
| World border → Disable 30,000,000 movement clamps | On | Remove the invisible walls |
| Spawn & teleport bounds → Relax spawn/teleport checks | On | /tp, /summon, height queries work beyond ±30M |
| Spawn & teleport bounds → Allow chunk generation everywhere | On | Remove chunk validity checks |
| Coordinate encoding → Extend section encoding (28/8/28) | On* | Push render/generation limit to the int32 edge (*auto-off under C2ME) |
| Far-lands stability → Fix chunk generation at the int edge | On | Wrap-aware worldgen distance/cache + edge guards (no OOM/freeze) |
| Far-lands stability → Guard huge move deltas | On | Prevent server-thread freeze |
| Far-lands stability → Entity/lighting/mineshaft/aquifer/octree overflow fixes | On | Crash guards |
| Far-lands stability → Disable structures far out | On | Avoid structure-overflow OOM |

## Feature details

1. **Fullbright (max light everywhere)** — `BlockAndLightGetter` / `LevelLightEngine` / `LightmapRenderStateExtractor` / `DarknessFogEnvironment`.
2. **Remove the built-in world border** — `WorldBorder` (wall/damage/vignette/clamping) + the three invisible walls: `Player#tick`, `ServerGamePacketListenerImpl#clampHorizontal`, `Entity#absSnapTo`. Huge move deltas are snapped instead of collided.
3. **Relax spawn & teleport checks** — `Level` bounds + `Level#getHeight`; `ChunkPos#isValid` allows generation anywhere.
4. **Extended coordinate encoding & stability patches**
   - `SectionPos` repacked **X/Z 28 bits + Y 8 bits** → render/generation limit moves to ±2,147,483,632 blocks (auto-fallback under C2ME)
   - `Aquifer`: long-math guard against absurd grid sizes (prevents OOM from huge allocations)
   - `LayerLightSectionStorage` / `EntitySectionStorage` / `MineshaftPieces` / `Octree` overflow guards
   - Structures skipped far out (avoid coordinate-overflow OOM)

## Build / Run

```bash
./gradlew build                      # produces both jars in one run:
                                     #   build/libs/farlandsprobe-26.2-<version>.jar
                                     #   mc-26.1.2/build/libs/farlandsprobe-26.1.2-<version>.jar
./gradlew build -x :mc-26.1.2:build  # 26.2 only
./gradlew :mc-26.1.2:build           # 26.1.2 only
./gradlew runClient                  # launches the 26.2 dev client
./gradlew :mc-26.1.2:runClient       # launches the 26.1.2 dev client
```

Install: put the jar matching your game version into `mods/`, requires Fabric Loader ≥ 0.19.3, **plus [Cloth Config](https://modrinth.com/mod/cloth-config)** (required); Mod Menu is optional (recommended for the config screen).

## Disclaimer

Terrain and worlds will be damaged unpredictably past the packing limits. **For exploration only — do not use on important saves.**
