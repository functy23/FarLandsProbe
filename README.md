# FarLands Probe (farlandsprobe)

**[English](README.md) | [简体中文](README.zh-CN.md)**

A Minecraft **26.2** Fabric mod (deobfuscated / Mojang official mappings).

> ⚠️ **Note: this mod does NOT restore the Far Lands.**
> The classic "Far Lands" is a terrain-generation bug from before Beta 1.8 that produced
> a huge noise wall around 12,550,824 blocks. This mod **does not** bring that terrain back.
> Instead it does the opposite: it **removes the modern world border and coordinate limits**
> (the 30,000,000-block border, the 33,554,432-block coordinate-encoding limit, etc.) so you
> can push past them and watch modern Minecraft's **precision start to break down** at extreme
> coordinates (lighting corruption, chunk structures overlapping, rendering vanishing, ...).
> If you want the authentic Far Lands terrain, look for a real Far Lands restoration mod.

All features are enabled by default, but **every feature can be toggled independently** — this
mod exists purely to explore the terrain breakdown beyond the world edge.

## Configuration

- **Config file**: `config/farlandsprobe.json` (auto-generated on first launch)
- **Config screen**: provided by **Cloth Config** (**required dependency**), grouped into five
  categories: Lighting / World border / Spawn & teleport bounds / Coordinate encoding /
  Far-lands stability
- **Mod Menu (optional)**: with Mod Menu installed, open the screen from
  Mods → FarLandsProbe → Config
- ⚠️ **Changing settings requires a game restart**: every patch hooks into worldgen,
  networking and renderer setup at startup; options marked with the "restart" badge only
  take effect after restarting the game

### Toggle overview

| Setting | Default | Description |
|---|---|---|
| Lighting → Fullbright (max light everywhere) | On | No darkness anywhere |
| World border → Remove world border | On | Remove border wall / damage / red vignette / clamping |
| World border → Disable 30,000,000 movement clamps | On | Remove the invisible wall |
| Spawn & teleport bounds → Relax spawn/teleport checks | On | Allow /tp, /summon and height queries anywhere |
| Spawn & teleport bounds → Allow chunk generation everywhere | On | Remove chunk validity checks |
| Coordinate encoding → Extend section encoding (28/8/28) | On | Push render/generation limit to the int32 edge |
| Far-lands stability → Guard huge move deltas | On | Prevent server thread freeze |
| Far-lands stability → Entity/lighting/mineshaft/aquifer/octree overflow fixes | On | Crash guards |
| Far-lands stability → Disable structures far out | On | Avoid structure-overflow OOM |

## Feature details

1. **Fullbright (max light everywhere)**
   - `BlockAndLightGetter#getBrightness` → always 15: every block/entity vertex light is maxed
   - `LevelLightEngine#getRawBrightness` → always 15: crop growth, mob-spawn checks, pathfinding and debug overlays all see full light
   - `LightmapRenderStateExtractor#extract`: lightmap sky/block factor = 15, `darknessEffectScale` = 0, gamma = 1
   - `DarknessFogEnvironment`: the darkness effect's fog is removed entirely

2. **Remove the built-in world border**
   - `WorldBorder#isWithinBounds(...)` → always true: border damage, entity-spawn gates and player interactions are all allowed
   - `WorldBorder#isInsideCloseToBorder` → false: the physical push wall disappears
   - `WorldBorder#getDistanceToBorder` → MAX_VALUE: the red warning vignette never appears
   - `WorldBorder#getCollisionShape` → empty collision shape: no code path can ever build a wall from the border box
   - `clampToBounds` / `clampVec3ToBound` → return the position unchanged: nothing is clamped back to the border
   - **`Player#tick` (invisible wall ① in 26.x)**: vanilla clamps the player's X/Z back to ±29,999,999 every tick, so you get blocked when walking out and yanked back the next tick after `/tp`. `PlayerMixin` removes both `Mth.clamp` calls with `@Redirect`.
   - **`ServerGamePacketListenerImpl#clampHorizontal` (invisible wall ②)**: the server clamps every move packet target to ±30,000,000, so **flying past is fine but walking / dropping out of flight snaps you back to 30,000,000**. `ServerGamePacketListenerImplMixin` removes that clamp (covers both player and vehicle movement).
   - **`Entity#absSnapTo` (invisible wall ③, the real "30 million" culprit)**: `handleMovePlayer` calls `absSnapTo` after every move, which clamps X/Z to ±30,000,000, pinning server entities back to 3.0E7 every tick (diagnostic logs show the client at 33,554,432.5 while the server snaps back to 30,000,000). `EntityMixin` removes that clamp.
   - **Huge move delta guard**: after coordinate encoding wraps, move packets can carry deltas of tens of millions of blocks; `Entity.move`'s `BlockCollisions` would scan an astronomical AABB and freeze the server thread (terrain stops loading, commands stop working, UI keeps rendering). `ServerGamePacketListenerImplMixin` `setPos`es straight to the target for deltas over 4096 blocks instead of colliding.

3. **Relax world generation & teleport checks**
   - `Level#isInSpawnableBounds` / `isInWorldBounds` / `isInValidBounds` → always true: /tp, /summon and block-coordinate arguments no longer reject positions beyond ±30,000,000
   - `Level#getHeight`: drops the ±30M branch and queries the generated chunks directly
   - `ChunkPos#isValid` → always true: removes the `GenerationChunkHolder` hard cap (~±33,553,360 blocks) so chunk generation continues

4. **Extended coordinate encoding & far-lands stability patches**
   - Vanilla `SectionPos.asLong` packs X/Z in 22 bits and Y in 20 bits, wrapping at 33,554,432 blocks and killing rendering/generation/lighting; `SectionPosMixin` repacks to **X/Z 28 bits + Y 8 bits** (world height 4064 blocks only needs 8), pushing the limit to ±2,147,483,632 blocks
   - `LayerLightSectionStorageMixin`: tolerates light sections missing due to `BlockPos.asLong` (26-bit X/Z) wrapping — skips updates instead of throwing NPE
   - `EntitySectionStorageMixin`: guards `start > end` overflow in section range queries (no crash)
   - `AquiferMixin` / `MineshaftPiecesMixin` / `ChunkGeneratorMixin`: mineshaft/structure int-overflow and absurd aquifer-grid OOM guards
   - `OctreeMixin`: the renderer `Octree` bounding box overflows at 2,147,483,296 (= 2^31-352) and the scene stops rendering; it is recomputed with long math and shifted back into int range

## What you should see

- 30,000,000 blocks: vanilla border position — border removed, keep going
- **33,554,432 blocks (vanilla coordinate-encoding limit, now broken through)**: the 26-bit blockNode wraps, corrupting lighting/block-level structures — the "breakdown" you are here to observe
- **~2,147,483,296 blocks (`Octree` render-tree overflow point)**: rendering keeps working to the world edge
- **±2,147,483,647 blocks (signed 32-bit int limit) = the absolute end**: `BlockPos`/`Vec3i` store coordinates in `int`, so 2^31-1 is the physical ceiling; beyond that `int` overflows negative. Going further needs MCBig-style arbitrary-precision coordinates (BlockPos as BigInteger), which is a full game fork, not a mod
- Exploring near the limit generates enormous amounts of terrain quickly and can exhaust memory: `build.gradle` already adds `-Xmx8G` to `runClient`

## Build / Run

```bash
./gradlew build      # produces build/libs/FarLandsProbe-26.2-1.0.0.jar
./gradlew runClient  # launches the dev client directly
```

To install in a normal client: put `build/libs/FarLandsProbe-26.2-1.0.0.jar` into `mods/`. Requires Fabric Loader ≥ 0.19.3,
**plus [Cloth Config](https://modrinth.com/mod/cloth-config)** (required); Mod Menu is optional (recommended for the config screen).

## Disclaimer

Terrain and worlds will be damaged in unpredictable ways once you pass the packing limits.
**For exploration only — do not use on important saves.**
