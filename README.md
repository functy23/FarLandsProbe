# FarLands Probe (farlandsprobe)

**[English](README.md) | [简体中文](README.zh-CN.md)**

A Minecraft **26.2** Fabric mod (deobfuscated / Mojang official mappings) for exploring what happens past the world edge, where precision starts to break down.

> ⚠️ **Note: this mod does NOT restore the Far Lands.** The classic "Far Lands" is a Beta-1.8-era noise-wall terrain bug at ~12,550,824 blocks. This mod does the opposite: it removes the modern world border and coordinate limits so you can push past them and observe precision corruption (lighting corruption, chunk structures overlapping, rendering vanishing, ...). Want the authentic Far Lands terrain? Look for a real Far Lands restoration mod.

All features are enabled by default, but **every feature can be toggled independently**.

## Configuration

- **Config file**: `config/farlandsprobe.json` (auto-generated on first launch)
- **Config screen**: provided by **Cloth Config** (**required**), grouped into five categories:
  Lighting / World border / Spawn & teleport bounds / Coordinate encoding / Far-lands stability
- **Mod Menu (optional)**: Mods → FarLandsProbe → Config
- ⚠️ **Changing settings requires a game restart** — every option carries the "restart" badge

## The 32-bit limit

> `BlockPos` stores coordinates in a signed 32-bit `int`, so ±2,147,483,647 is the hard ceiling. The extended section packing (X22/Y22/Z20) pushes the *generated* range to **X and Y: ±33,554,432 blocks** (Y is enough for the sky far lands around 25,101,647) and **Z: ±8,388,608 blocks**. Past those, section coordinates wrap; going truly beyond would require arbitrary-precision coordinates (a whole game fork, e.g. MCBig-style), which is not possible in a mod.

## C²M (C2ME) compatibility

> ⚠️ **C²M Engine rewrites the chunk storage/async loading system and is incompatible with the extended 22/22/20 section packing.** When C2ME is detected, the mod **automatically falls back to vanilla section encoding** (log: `C²M Engine detected: 22/22/20 section encoding auto-disabled`). In a C2ME profile you can still remove the border / movement clamps / teleport bounds, but far-lands deep exploration (beyond ±33,554,432 blocks) will not generate. Use a **non-C2ME environment** for that.

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
| Coordinate encoding → Extend section encoding (X22/Y22/Z20) | On* | X/Y to ±33.5M blocks, Z to ±8.4M (*auto-off under C2ME) |
| Far-lands stability → Guard huge move deltas | On | Prevent server-thread freeze |
| Far-lands stability → Entity/lighting/mineshaft/aquifer/octree overflow fixes | On | Crash guards |
| Far-lands stability → Disable structures far out | On | Avoid structure-overflow OOM |

## Feature details

1. **Fullbright (max light everywhere)** — `BlockAndLightGetter` / `LevelLightEngine` / `LightmapRenderStateExtractor` / `DarknessFogEnvironment`.
2. **Remove the built-in world border** — `WorldBorder` (wall/damage/vignette/clamping) + the three invisible walls: `Player#tick`, `ServerGamePacketListenerImpl#clampHorizontal`, `Entity#absSnapTo`. Huge move deltas are snapped instead of collided.
3. **Relax spawn & teleport checks** — `Level` bounds + `Level#getHeight`; `ChunkPos#isValid` allows generation anywhere.
4. **Extended coordinate encoding & stability patches**
   - `SectionPos` repacked **X 22 / Y 22 / Z 20 bits** → X and Y both reach ±33,554,432 blocks (Y reaches the sky far lands ~25,101,647), Z reaches ±8,388,608 (auto-fallback under C2ME)
   - `Aquifer`: long-math guard against absurd grid sizes (prevents OOM from huge allocations)
   - `LayerLightSectionStorage` / `EntitySectionStorage` / `MineshaftPieces` / `Octree` overflow guards
   - Structures skipped far out (avoid coordinate-overflow OOM)

## Build / Run

```bash
./gradlew build      # produces build/libs/FarLandsProbe-26.2-<version>.jar
./gradlew runClient  # launches the dev client directly
```

Install: put the jar into `mods/`, requires Fabric Loader ≥ 0.19.3, **plus [Cloth Config](https://modrinth.com/mod/cloth-config)** (required); Mod Menu is optional (recommended for the config screen).

## Disclaimer

Terrain and worlds will be damaged unpredictably past the packing limits. **For exploration only — do not use on important saves.**
