<div align="center">

# 🧭 FarLands Probe

**一个 Minecraft 26.3 / 26.2 / 26.1.2 的 Fabric 模组：移除世界边界与坐标上限，去世界边缘之外观察精度崩坏。**

[![FarLandsProbe](https://img.shields.io/badge/FarLandsProbe-FLP-orange.svg)](https://github.com/functy23/FarLandsProbe)
[![Java](https://img.shields.io/badge/Java-25-red.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Top Language](https://img.shields.io/github/languages/top/functy23/FarLandsProbe?style=flat)](https://github.com/functy23/FarLandsProbe)
[![Platform](https://img.shields.io/badge/platform-Fabric%20%7C%20Minecraft%2026.x-lightgrey.svg?logo=minecraft&logoColor=white)](https://github.com/functy23/FarLandsProbe)

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg?logo=opensourceinitiative&logoColor=white)](https://opensource.org/licenses/MIT)

[![Release](https://img.shields.io/github/v/release/functy23/FarLandsProbe?style=flat&logo=github)](https://github.com/functy23/FarLandsProbe/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/functy23/FarLandsProbe/total?label=Downloads&logo=github)](https://github.com/functy23/FarLandsProbe/releases)
[![Stars](https://img.shields.io/github/stars/functy23/FarLandsProbe?style=flat&logo=github)](https://github.com/functy23/FarLandsProbe/stargazers)
[![Repo Size](https://img.shields.io/github/repo-size/functy23/FarLandsProbe?style=flat&logo=github)](https://github.com/functy23/FarLandsProbe)
[![Contributors](https://img.shields.io/github/contributors/functy23/FarLandsProbe?color=ee8449&logo=githubsponsors)](https://github.com/functy23/FarLandsProbe/graphs/contributors)

[Issues](https://github.com/functy23/FarLandsProbe/issues) • [Releases](https://github.com/functy23/FarLandsProbe/releases)

[English](../README.md) | **简体中文**
</div>

---

一个面向 Minecraft **26.3 / 26.2 / 26.1.2** 的 Fabric 模组（反混淆 / Mojang 官方映射），用于探索世界边界之外、精度开始崩溃时会发生什么。同一份源码树可为三个游戏版本构建。

> ⚠️ **注意：本模组不会还原远地（Far Lands）。** 经典的「远地」是 Beta 1.8 时代 ~12,550,824 格处的噪声墙地形 bug。本模组做的恰好相反：它移除现代世界边界与坐标上限，让你能越过去观察精度损坏（光照错乱、区块结构重叠、渲染消失……）。想要真正的远地地形？请去找专门的远地还原模组。

所有功能默认开启，但**每一项都可以单独开关**。

## Changelog

**v1.3.0** — 支持 Minecraft 26.3：
- 新增 **Minecraft 26.3** 构建（`mc-26.3`）。26.3 重构了含水层创建：`Aquifer.create(NoiseChunk, ChunkPos, NoiseRouter, ...)` 变为 `Aquifer$Config.create(DensitySamplerSet, PositionalRandomFactory, DensityVolume, FluidPicker)`，因此含水层溢出防护有了 26.3 版本（`AquiferConfigMixin`，只在 26.3 的 mixin 配置里注册）。网格公式本身（X/Z `>> 4`、Y `floorDiv(v, 12)`、再三边连乘）没变，于是把判定抽到共享的 `AquiferGridGuard`（有单元测试），两个版本只差注入点。**其余所有 mixin 目标在 26.1.2 / 26.2 / 26.3 上 API 完全一致。**
- 26.3 依赖：Fabric Loader 0.19.5、Fabric API 0.160.7+26.3、Cloth Config 26.3.158+fabric、Mod Menu 21.0.0-beta.1。
- 新增 `./gradlew verifyMixins` 任务：静态校验每个已注册 mixin 的目标类 / 目标方法 / `@At` 调用点 / `@Shadow`，以及 **mixin 自身的 class/interface 类型是否与目标一致**。javac 不校验注入点，而 `defaultRequire = 1` 会让一个失效目标在运行时废掉整份 mixin 配置——本任务把「这次升级有没有改坏 mixin」变成构建步骤。当前结果：**每个版本 81 项检查，0 错误**。这次适配 26.3 时它抓出了两个真实问题：含水层入口搬家、以及一个 mixin 用 interface 打 class 目标（只做静态方法校验是发现不了后者的）。
- 新增 `./gradlew smokeMixinLoad` 任务：把每个版本的开发服务端真正启动到「Mixin 加载器接受这份配置」，再停掉（会自己在 gitignore 的 `run/` 目录里补上 `eula=true`）。这是静态校验看不到的那一层——26.3 目前能带着 mod 跑到 `Done (0.228s)`，零 mixin 报错。

**v1.2.1** — 维护性清理（无行为变更）：
- 全部 Java 注释统一为中文（原中英混杂）。
- 回绕周期 2^28 去重：统一到 `SectionEncoding#WRAP_PERIOD`（原在 `WorldGenRegionMixin`/`StaticCache2DMixin` 各定义一份）。
- `WorldGenRegion#wrapChunkRequest` 降嵌套：偏移数组静态化 + 提取 `lookupWrappedChunk` 辅助方法，循环嵌套降到 3 层。
- `FarLandsProbeConfig`：每个开关补充「功能 → mixin」映射注释，README 不再是理解功能全貌的唯一索引。

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

## The 32-bit limit

> `BlockPos` 用有符号 32 位 `int` 存坐标，所以 **±2,147,483,647 是物理上限**。扩展的坐标编码把渲染/生成上限推到 int 边缘，边界防御保证这里稳定生成。**越过之后，坐标在 `int` 里回绕，地形不再生成**——世界不会折叠，只是安静停住。要再往前需要任意精度坐标（整套游戏 fork，例如 MCBig 的思路），mod 层面做不到。

## C²M (C2ME) compatibility

> ⚠️ **C²M 引擎重写了区块存储/异步加载系统，与扩展的 28/8/28 坐标编码不兼容。** 检测到 C2ME 时，本 mod **会自动回落到 vanilla 编码**（日志提示 `C²M Engine detected: 28/8/28 section encoding auto-disabled`）。在 C2ME 配置里你仍可使用移除边境、移动钳制、传送边界等功能，但 far lands 深度探索（越过 ±33,554,432 格）无法生成。要看完整的 far lands 崩坏，请用**无 C2ME 的环境**。

## Rendering caveats

> ⚠️ 在 ±2,147,483,647 附近，渲染可能出现：**区块闪烁/闪动、区块忽隐忽现、光照闪烁、方块短暂消失**（渲染八叉树和坐标编码正顶着 int 极限工作）。这是预期的——本 mod 就是在探索崩坏，而不是打磨它。

## Toggle overview

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

## Feature details

1. **全图无黑暗**：`BlockAndLightGetter` / `LevelLightEngine` / `LightmapRenderStateExtractor` / `DarknessFogEnvironment`
2. **移除世界边境**：`WorldBorder`（墙/伤害/红幕/钳制）+ 三道隐形墙（`Player#tick`、`clampHorizontal`、`absSnapTo`）；超大移动增量直接 `setPos` 而不是碰撞
3. **放开生成/传送检查**：`Level` 边界 + `getHeight`；`ChunkPos#isValid` 允许任意坐标生成
4. **扩展坐标编码与稳定性修复**
   - `SectionPos` 改为 **X/Z 28 位 + Y 8 位** → 渲染/生成上限推到 ±2,147,483,632 格（C2ME 下自动回落）
   - `Aquifer`：long 计算防御异常的网格尺寸（防巨大数组 OOM）——26.1.2/26.2 走 `AquiferMixin`，26.3 走 `AquiferConfigMixin`（见更新日志），判定逻辑共享 `AquiferGridGuard`
   - `LayerLightSectionStorage` / `EntitySectionStorage` / `MineshaftPieces` / `Octree` 溢出防护
   - 极远坐标跳过结构生成（避免坐标溢出 OOM）

## Build / Run

```bash
./gradlew build                    # 一次产出三个版本：
                                   #   build/libs/farlandsprobe-26.2-<version>.jar
                                   #   mc-26.1.2/build/libs/farlandsprobe-26.1.2-<version>.jar
                                   #   mc-26.3/build/libs/farlandsprobe-26.3-<version>.jar
./gradlew verifyMixins             # 静态校验三个版本的 mixin 注入点
./gradlew smokeMixinLoad           # 运行期冒烟：启动三个版本的开发服务端，证明 mixin 配置能被加载
./gradlew build -x :mc-26.1.2:build -x :mc-26.3:build   # 只构建 26.2
./gradlew :mc-26.3:build           # 只构建 26.3
./gradlew :mc-26.1.2:build         # 只构建 26.1.2
./gradlew runClient                # 启动 26.2 开发客户端
./gradlew :mc-26.3:runClient       # 启动 26.3 开发客户端
./gradlew :mc-26.1.2:runClient     # 启动 26.1.2 开发客户端
```

装入普通客户端：把对应游戏版本的 jar 放进 `mods/`，需要 Fabric Loader ≥ 0.19.3（26.3 需 ≥ 0.19.5），**并同时安装 [Cloth Config](https://modrinth.com/mod/cloth-config)**（必装）；Mod Menu 可选（推荐，用于打开配置界面）。

## Disclaimer

越过打包上限后地形/存档会以不可预期方式损坏。**请只用于探索，勿在重要存档上使用**。
