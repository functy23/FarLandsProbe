# FarLands Probe (farlandsprobe)

**[English](README.md) | [简体中文](README.zh-CN.md)**

一个 Minecraft **26.2 / 26.1.2** Fabric 模组（无混淆 / Mojang 官方映射），用于探索世界边界之外、精度开始崩坏的地形。同一份源码同时构建两个游戏版本。

> ⚠️ **注意：本 mod 并非「恢复边境之地（Far Lands）」。** 经典「边境之地」是 Beta 1.8 之前地形生成器在 12,550,824 格处产生的海量噪声地形 bug，本 mod **不会**恢复那种地形。它做的是相反的事：**移除现代版本的世界边境与坐标限制**，让你越过限制直接观察精度崩坏的现象（光照错乱、区块结构覆盖、渲染消失等）。想要原汁原味的边境之地地形，请去找真正的 Far Lands 恢复类 mod。

所有功能默认开启，但**每个功能都可以独立开关**。

## 更新日志

**v1.2.0** — 多版本支持：
- 新增 **Minecraft 26.1.2** 构建（26.1.2 与 26.2 对所有 mixin 目标 API 完全一致，源码零差异），与 26.2 共用同一份源码。
- 构建改为双子项目（根 = 26.2，`mc-26.1.2` = 26.1.2），`./gradlew build` 一次产出两个 jar。

**v1.1.2** — 恢复边界稳定性修复，并清理代码库：
- 重新加入边界区块生成防护（`WorldGenRegion`/`StaticCache2D` 回绕处理、边界 feature 跳过、寻路跳过、aquifer int 边界防御），在接近/越过 int32 边界生成时**不再 OOM 或卡死**——但**没有坐标回绕**：越过 ±2,147,483,647 后地形停止生成，游戏保持响应。
- 矿井恢复生成（删除全局禁用数据包；溢出安全中点补丁覆盖远地坐标）。
- HMAP 驱动的清理：fullbright 集中化、新增单元测试（11 个通过）、魔法数字补注释、新增 `.editorconfig`、support 类移出 mixin 包（修复一个世界生成崩溃）。

**v1.1.1** — 删除所有试图在 32 位上限之外生成区块的代码，并撤销 Y 轴扩展实验：
- 坐标编码恢复为 **X/Z 28 位 + Y 8 位**（X/Z 可达 int32 极限 ±2,147,483,632 格；Y 保持世界高度范围）
- 移除坐标回绕、边界区块生成 hack（WorldGenRegion/StaticCache2D）、边界 feature 跳过、寻路跳过
- **±2,147,483,647 是硬终点**：越过之后坐标在 `int` 里回绕，地形无法生成——这是物理上限
- 保留 C2ME 自动兼容（检测到 C2ME 时回落到 vanilla 编码）

## 配置

- **配置文件**：`config/farlandsprobe.json`（启动时自动生成）
- **配置界面**：由 **Cloth Config** 提供（**必装依赖**），分「光照 / 世界边境 / 生成与传送边界 / 坐标编码 / 远地稳定性修复」五类
- **Mod Menu（可选）**：模组列表 → FarLandsProbe → 配置
- ⚠️ **修改配置后需要重启游戏才能生效**（界面带「重启」标记）

## 32 位上限

> `BlockPos` 用有符号 32 位 `int` 存坐标，所以 **±2,147,483,647 是物理上限**。扩展的坐标编码把渲染/生成上限推到 int 边缘，边界防御保证这里稳定生成。**越过之后，坐标在 `int` 里回绕，地形不再生成**——世界不会折叠，只是安静停住。要再往前需要任意精度坐标（整套游戏 fork，例如 MCBig 的思路），mod 层面做不到。

## C²M（C2ME）兼容性

> ⚠️ **C²M 引擎重写了区块存储/异步加载系统，与扩展的 28/8/28 坐标编码不兼容。** 检测到 C2ME 时，本 mod **会自动回落到 vanilla 编码**（日志提示 `C²M Engine detected: 28/8/28 section encoding auto-disabled`）。在 C2ME 配置里你仍可使用移除边境、移动钳制、传送边界等功能，但 far lands 深度探索（越过 ±33,554,432 格）无法生成。要看完整的 far lands 崩坏，请用**无 C2ME 的环境**。

## 渲染提示

> ⚠️ 在 ±2,147,483,647 附近，渲染可能出现：**区块闪烁/闪动、区块忽隐忽现、光照闪烁、方块短暂消失**（渲染八叉树和坐标编码正顶着 int 极限工作）。这是预期的——本 mod 就是在探索崩坏，而不是打磨它。

## 功能开关总览

| 配置项 | 默认 | 说明 |
|---|---|---|
| 光照 → 无黑暗（全局最高亮度） | 开 | 全图无黑暗 |
| 世界边境 → 移除世界边境 | 开 | 墙/伤害/红幕/钳制移除 |
| 世界边境 → 禁用 30,000,000 移动钳制 | 开 | 移除隐形墙 |
| 生成与传送边界 → 放开生成/传送检查 | 开 | /tp、/summon、高度查询放行 |
| 生成与传送边界 → 允许任意坐标生成区块 | 开 | 解除区块合法性检查 |
| 坐标编码 → 扩展区块坐标编码（28/8/28） | 开* | 渲染/生成上限推到 int32 极限（*C2ME 下自动关闭） |
| 远地稳定性 → 修复 int 边界区块生成 | 开 | 回绕感知的世界生成距离/缓存 + 边界防御（不 OOM/不卡死） |
| 远地稳定性 → 阻止超大移动增量卡死 | 开 | 防服务器线程假死 |
| 远地稳定性 → 实体/光照/矿井/aquifer/八叉树溢出修复 | 开 | 抗崩溃补丁 |
| 远地稳定性 → 极远坐标禁用结构生成 | 开 | 避免结构溢出 OOM |

## 功能细节

1. **全图无黑暗**：`BlockAndLightGetter` / `LevelLightEngine` / `LightmapRenderStateExtractor` / `DarknessFogEnvironment`
2. **移除世界边境**：`WorldBorder`（墙/伤害/红幕/钳制）+ 三道隐形墙（`Player#tick`、`clampHorizontal`、`absSnapTo`）；超大移动增量直接 `setPos` 而不是碰撞
3. **放开生成/传送检查**：`Level` 边界 + `getHeight`；`ChunkPos#isValid` 允许任意坐标生成
4. **扩展坐标编码与稳定性修复**
   - `SectionPos` 改为 **X/Z 28 位 + Y 8 位** → 渲染/生成上限推到 ±2,147,483,632 格（C2ME 下自动回落）
   - `Aquifer`：long 计算防御异常的网格尺寸（防巨大数组 OOM）
   - `LayerLightSectionStorage` / `EntitySectionStorage` / `MineshaftPieces` / `Octree` 溢出防护
   - 极远坐标跳过结构生成（避免坐标溢出 OOM）

## 构建 / 运行

```bash
./gradlew build                    # 一次产出两个版本：
                                   #   build/libs/farlandsprobe-26.2-<version>.jar
                                   #   mc-26.1.2/build/libs/farlandsprobe-26.1.2-<version>.jar
./gradlew build -x :mc-26.1.2:build   # 只构建 26.2
./gradlew :mc-26.1.2:build        # 只构建 26.1.2
./gradlew runClient               # 启动 26.2 开发客户端
./gradlew :mc-26.1.2:runClient    # 启动 26.1.2 开发客户端
```

装入普通客户端：把对应游戏版本的 jar 放进 `mods/`，需要 Fabric Loader ≥ 0.19.3，**并同时安装 [Cloth Config](https://modrinth.com/mod/cloth-config)**（必装）；Mod Menu 可选（推荐，用于打开配置界面）。

## 免责声明

越过打包上限后地形/存档会以不可预期方式损坏。**请只用于探索，勿在重要存档上使用**。
