# FarLands Probe (farlandsprobe)

Minecraft **26.2** Fabric mod（无混淆 / Mojang 官方映射）。

三个功能，全部是为了**探索世界边界之外、精度开始崩坏的地形**：

1. **全图无黑暗（Fullbright）**
   - `BlockAndLightGetter#getBrightness` → 恒 15：所有方块/实体顶点光照拉满
   - `LevelLightEngine#getRawBrightness` → 恒 15：作物生长、刷怪光照判断、寻路、调试面板全部认为最亮
   - `LightmapRenderStateExtractor#extract`：光贴图 sky/block factor = 15、`darknessEffectScale` = 0、gamma = 1
   - `DarknessFogEnvironment`：黑暗效果的雾完全移除

2. **移除自带世界边境**
   - `WorldBorder#isWithinBounds(...)` → 恒 true：边境伤害、实体生成门、玩家交互全部放行
   - `WorldBorder#isInsideCloseToBorder` → false：物理推挤墙消失
   - `WorldBorder#getDistanceToBorder` → MAX_VALUE：红色警告幕布不出现
   - `WorldBorder#getCollisionShape` → 空碰撞体：任何代码路径都无法用边境碰撞盒砌墙
   - `clampToBounds` / `clampVec3ToBound` → 原样返回：不再把位置钳回边境
   - **`Player#tick`（26.x 的隐形墙 ①）**：原版每 tick 把玩家 X/Z 钳回 ±29,999,999，
     走过去会被挡、`/tp` 过去下一 tick 又被拉回。`PlayerMixin` 用 `@Redirect`
     取消这两处 `Mth.clamp`。
   - **`ServerGamePacketListenerImpl#clampHorizontal`（26.x 的隐形墙 ②）**：服务端
     把每个移动包的目标坐标钳到 ±30,000,000，所以**飞行越过没问题、一旦走路/脱离飞行
     就会被拉回 30,000,000**。`ServerGamePacketListenerImplMixin` 取消该钳制
     （同时覆盖玩家移动与载具移动）。
   - **`Entity#absSnapTo`（26.x 的隐形墙 ③，真正的"三千万格"元凶）**：
     `handleMovePlayer` 每次移动后都调用 `absSnapTo`，而它内部把 X/Z 钳到 ±30,000,000，
     导致服务器实体每 tick 被钉回 3.0E7（诊断日志实证：客户端在 33,554,432.5、
     服务器每次回到 30,000,000）。`EntityMixin` 解除该钳制。
   - **巨大移动增量防护**：坐标编码回绕后移动包可能出现几百万格的 delta，
     `Entity.move` 的 `BlockCollisions` 会遍历天文数字方块导致服务器线程假死
     （地形不加载、指令失灵、UI 正常）。`ServerGamePacketListenerImplMixin` 对
     超过 4096 格的 delta 直接 `setPos` 到位而不是走碰撞。

3. **放开世界生成与传送检查**
   - `Level#isInSpawnableBounds` / `isInWorldBounds` / `isInValidBounds` → 恒 true：/tp、/summon、方块坐标参数不再拒绝 ±30,000,000 之外
   - `Level#getHeight`：去掉 ±30M 分支，直接查询已生成区块
   - `ChunkPos#isValid` → 恒 true：解除 `GenerationChunkHolder` 的硬上限（约 ±33,553,360 格），让区块继续生成

## 预期观察到的现象

- 30,000,000 格：原版边境位置，边境已移除，可继续前进
- **33,554,432 格（原版坐标编码极限，现已被突破）**：
  - `SectionPos.asLong` 原版 X/Z 22 位、Y 20 位，此处回绕导致渲染/生成/光照全部失效
  - `SectionPosMixin` 改为 **X/Z 28 位 + Y 8 位**（世界高度 4064 格只需 8 位），
    渲染/生成上限提升到 ±2,147,483,632 格（约 21 亿格）
  - `LayerLightSectionStorageMixin`：`BlockPos.asLong` 的 blockNode 仍是 26 位 X/Z，
    在 33,554,432 回绕会让光照引擎拿到不存在的 section 而 NPE；改为对缺失光照层容忍
    （跳过更新），回绕区光照自然缺失/错乱
  - `EntitySectionStorageMixin`：section 范围查询 `start > end` 溢出保护（不崩溃）
- **更远处（直到约 21 亿格）**：blockNode（26 位）回绕导致光照/方块级结构错乱 =
  你要观察的"崩坏"；超过 21 亿格后 section 28 位也回绕，各类 long 键结构开始互相覆盖
- **~2,147,483,296 格（`Octree` 渲染树溢出点）**：渲染器的 `Octree` 用
  `minX + 512 - 1` 计算包围盒，在 2,147,483,296（= 2^31-352）处溢出成负数导致
  整个场景停止渲染。`OctreeMixin` 用 long 计算并平移钳进 int 范围（保持 512 幂次跨度），
  让渲染能一直工作到世界边缘。
- **±2,147,483,647 格（有符号 32 位 int 极限）= 绝对终点**：
  - `BlockPos`/`Vec3i` 用 `int` 存坐标，2^31-1 是物理上限，之后 `int` 溢出为负数
  - 本 mod 的 section 28 位打包恰好设计到此处（最后一个合法 section = 2^27-1）
  - 越过需要 MCBig 式任意精度坐标（把 BlockPos 改成 BigInteger），那是整套游戏 fork，不是 mod
  - 探索到极限附近会短时间生成海量区块，容易堆内存耗尽：`build.gradle` 已给
    `runClient` 加 `-Xmx8G`（16G 内存机器）

## 构建 / 运行

```bash
./gradlew build      # 产出 build/libs/FarLandsProbe-26.2-1.0.0.jar
./gradlew runClient  # 直接启动开发客户端
```

装入普通客户端：把 `build/libs/FarLandsProbe-26.2-1.0.0.jar` 放进 `mods/`，需要 Fabric Loader ≥ 0.19.3。

## 免责声明

越过打包上限后地形/存档会以不可预期方式损坏。**请只用于探索，勿在重要存档上使用**。
