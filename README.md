# FarLands Probe (farlandsprobe)

Minecraft **26.2** Fabric mod（无混淆 / Mojang 官方映射）。

> ⚠️ **注意：本 mod 并非「恢复边境之地（Far Lands）」。**
> 经典「边境之地」是 Beta 1.8 之前地形生成器在 12,550,824 格处产生的海量噪声地形 bug，
> 本 mod **不会**恢复那种地形。它做的是相反的事：**移除现代版本的世界边境与坐标限制**
> （30,000,000 格边境、33,554,432 格坐标编码极限等），让你能越过这些限制，直接观察
> 现代 Minecraft 在极端坐标下**精度开始崩坏**的现象（光照错乱、区块结构覆盖、渲染消失等）。
> 想要原汁原味的边境之地地形，请去找真正的 Far Lands 恢复类 mod。

所有功能默认开启，但**每个功能都可以独立开关**，全部是为了探索世界边界之外、
精度开始崩坏的地形。

## 配置

- **配置文件**：`config/farlandsprobe.json`（游戏启动时自动生成）
- **配置界面**：由 **Cloth Config** 提供（**必装依赖**），分「光照 / 世界边境 /
  生成与传送边界 / 坐标编码 / 远地稳定性修复」五类开关
- **Mod Menu（可选）**：装了 Mod Menu 后，可从 模组列表 → FarLandsProbe →
  配置 打开配置界面
- ⚠️ **修改配置后需要重启游戏才能生效**：所有补丁都作用于世界生成、网络与渲染初始化
  阶段，配置界面中带「重启」标记的选项改动后请重启游戏

### 功能开关总览

| 配置项 | 默认 | 说明 |
|---|---|---|
| 光照 → 无黑暗（全局最高亮度） | 开 | 全图无黑暗 |
| 世界边境 → 移除世界边境 | 开 | 移除自带边境（墙/伤害/红幕/钳制） |
| 世界边境 → 禁用 30,000,000 移动钳制 | 开 | 解除隐形墙 |
| 生成与传送边界 → 放开生成/传送检查 | 开 | /tp、/summon、高度查询放行 |
| 生成与传送边界 → 允许任意坐标生成区块 | 开 | 解除区块合法性检查 |
| 坐标编码 → 扩展区块坐标编码（28/8/28） | 开 | 渲染/生成上限推到 int32 极限 |
| 远地稳定性修复 → 阻止超大移动增量卡死 | 开 | 防服务器线程假死 |
| 远地稳定性修复 → 实体/光照/矿井/aquifer/八叉树溢出修复 | 开 | 抗崩溃补丁 |
| 远地稳定性修复 → 极远坐标禁用结构生成 | 开 | 避免结构溢出引发 OOM |

## 功能细节

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

4. **扩展坐标编码与远地稳定性修复**
   - `SectionPos.asLong` 原版 X/Z 22 位、Y 20 位，在 33,554,432 格回绕导致渲染/生成/光照失效；
     `SectionPosMixin` 改为 **X/Z 28 位 + Y 8 位**（世界高度 4064 格只需 8 位），
     渲染/生成上限提升到 ±2,147,483,632 格
   - `LayerLightSectionStorageMixin`：对 `BlockPos.asLong`（26 位 X/Z）回绕导致的缺失光照 section 容忍（跳过更新），避免 NPE
   - `EntitySectionStorageMixin`：section 范围查询 `start > end` 溢出保护（不崩溃）
   - `AquiferMixin` / `MineshaftPiecesMixin` / `ChunkGeneratorMixin`：矿井/结构坐标 int 溢出与 aquifer 巨大网格 OOM 防护
   - `OctreeMixin`：渲染器 `Octree` 包围盒在 2,147,483,296（= 2^31-352）处溢出导致停止渲染；用 long 计算并平移钳进 int 范围

## 预期观察到的现象

- 30,000,000 格：原版边境位置，边境已移除，可继续前进
- **33,554,432 格（原版坐标编码极限，现已被突破）**：blockNode（26 位）回绕导致光照/方块级结构错乱 = 你要观察的"崩坏"
- **~2,147,483,296 格（`Octree` 渲染树溢出点）**：渲染一直工作到世界边缘
- **±2,147,483,647 格（有符号 32 位 int 极限）= 绝对终点**：`BlockPos`/`Vec3i` 用 `int` 存坐标，2^31-1 是物理上限，之后 `int` 溢出为负数；越过需要 MCBig 式任意精度坐标（把 BlockPos 改成 BigInteger），那是整套游戏 fork，不是 mod
- 探索到极限附近会短时间生成海量区块，容易堆内存耗尽：`build.gradle` 已给 `runClient` 加 `-Xmx8G`

## 构建 / 运行

```bash
./gradlew build      # 产出 build/libs/FarLandsProbe-26.2-1.0.0.jar
./gradlew runClient  # 直接启动开发客户端
```

装入普通客户端：把 `build/libs/FarLandsProbe-26.2-1.0.0.jar` 放进 `mods/`，需要 Fabric Loader ≥ 0.19.3，
**并同时安装 [Cloth Config](https://modrinth.com/mod/cloth-config)**（必装）；Mod Menu 可选（推荐，用于打开配置界面）。

## 免责声明

越过打包上限后地形/存档会以不可预期方式损坏。**请只用于探索，勿在重要存档上使用**。
