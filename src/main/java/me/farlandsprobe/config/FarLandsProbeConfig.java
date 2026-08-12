package me.farlandsprobe.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * FarLandsProbe 配置,由 Cloth Config 的 AutoConfig 驱动。
 *
 * 每个功能都是一个布尔开关,默认全部为 {@code true},以保持老用户"全开"的历史行为。
 * 文件存放在 {@code config/farlandsprobe.json}。
 *
 * 所有字段都标注了 {@link ConfigEntry.Gui.RequiresRestart},因为这些补丁在启动时
 * 就挂进了世界生成/网络/渲染器流程,改动要下次启动才生效。
 *
 * 每个开关后的注释给出了它控制的 mixin 映射(功能 → mixin 对应关系也见 README 的
 * "功能开关总览"与 "Feature details" 两节)。
 */
@Config(name = "farlandsprobe")
public class FarLandsProbeConfig implements ConfigData {

	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Lighting lighting = new Lighting();

	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Border border = new Border();

	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Bounds bounds = new Bounds();

	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Encoding encoding = new Encoding();

	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Stability stability = new Stability();

	/** 光照 / Lighting */
	public static class Lighting {
		/**
		 * 全亮(所有位置最大亮度)。
		 * mixin:BlockAndLightGetterMixin、LevelLightEngineMixin,
		 * client/LightmapRenderStateExtractorMixin、client/DarknessFogEnvironmentMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fullBright = true;
	}

	/** 世界边境 / World border */
	public static class Border {
		/**
		 * 移除世界边界(墙/伤害/红色暗角/夹取)。
		 * mixin:WorldBorderMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean removeWorldBorder = true;

		/**
		 * 禁用 30,000,000 移动夹取(隐形墙)。
		 * mixin:PlayerMixin、EntityMixin、ServerGamePacketListenerImplMixin(clampHorizontal);
		 * 共享逻辑见 support/MovementClampSupport。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean disableMovementClamps = true;
	}

	/** 生成与传送边界 / Spawn & teleport bounds */
	public static class Bounds {
		/**
		 * 放宽生成/传送/高度查询边界。
		 * mixin:LevelMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean relaxSpawnAndTeleportBounds = true;

		/**
		 * 允许任意位置生成区块。
		 * mixin:ChunkPosMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean allowChunkGenerationEverywhere = true;
	}

	/** 坐标编码 / Coordinate encoding */
	public static class Encoding {
		/**
		 * 扩展 section 编码(28/8/28),把渲染/生成上限推到 int32 边界。
		 * mixin:SectionPosMixin;位运算见 support/SectionEncoding。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean extendSectionEncoding = true;
	}

	/** 远地稳定性修复 / Far-lands stability patches */
	public static class Stability {
		/**
		 * 修复 int 边界的区块生成(回绕感知的距离/缓存 + 边界防护,防 OOM/冻结)。
		 * mixin:WorldGenRegionMixin、StaticCache2DMixin、ChunkGeneratorMixin(特征跳过)、
		 * PathNavigationMixin、WorldGenRegionMixin#getHeight 防护。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixChunkBoundaryGeneration = true;

		/**
		 * 防护巨大的移动位移,避免服务器线程冻结。
		 * mixin:ServerGamePacketListenerImplMixin(guardHugeMoveDelta)。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean guardHugeMoveDelta = true;

		/**
		 * 实体 section 溢出防护(范围退化而非崩溃)。
		 * mixin:EntitySectionStorageMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixEntitySectionOverflow = true;

		/**
		 * 光照 section 崩溃防护(缺失数据跳过而非 NPE)。
		 * mixin:LayerLightSectionStorageMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixLightSectionCrash = true;

		/**
		 * 废弃矿井坐标溢出修复(防溢出中点)。
		 * mixin:MineshaftPiecesMixin;算术见 support/OverflowSafeMath。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixMineshaftOverflow = true;

		/**
		 * 含水层溢出防护(荒谬网格 → 禁用含水层,防分配 OOM)。
		 * mixin:AquiferMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixAquiferOverflow = true;

		/**
		 * 渲染八叉树溢出防护(包围盒平移回 int 范围)。
		 * mixin:client/OctreeMixin。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixOctreeOverflow = true;

		/**
		 * 远处禁用结构生成(防坐标溢出 OOM)。
		 * mixin:ChunkGeneratorMixin(createStructures 跳过)。
		 */
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean disableStructuresFarOut = true;
	}

	private static Boolean c2mePresent;

	private static boolean isC2mePresent() {
		if (c2mePresent == null) {
			c2mePresent = net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("c2me");
		}
		return c2mePresent;
	}

	/**
	 * C²M 引擎重写了区块存储/异步加载系统，基于原版 22/20/22 的 section 打包，
	 * 与 28/8/28 坐标扩展不兼容（遇到 int 边界回绕区块会 "Max retries reached" 崩溃）。
	 * 检测到 c2me 时自动回落到原版编码。
	 */
	public static boolean isC2meCompatMode() {
		return isC2mePresent();
	}

	public static void register() {
		AutoConfig.register(FarLandsProbeConfig.class, GsonConfigSerializer::new);
	}

	public static void save() {
		AutoConfig.getConfigHolder(FarLandsProbeConfig.class).save();
	}

	public static FarLandsProbeConfig get() {
		return AutoConfig.getConfigHolder(FarLandsProbeConfig.class).getConfig();
	}

	public static boolean isFullBright() {
		return get().lighting.fullBright;
	}

	public static boolean isRemoveWorldBorder() {
		return get().border.removeWorldBorder;
	}

	public static boolean isDisableMovementClamps() {
		return get().border.disableMovementClamps;
	}

	public static boolean isRelaxSpawnAndTeleportBounds() {
		return get().bounds.relaxSpawnAndTeleportBounds;
	}

	public static boolean isAllowChunkGenerationEverywhere() {
		return get().bounds.allowChunkGenerationEverywhere;
	}

	public static boolean isExtendSectionEncoding() {
		if (isC2mePresent()) {
			return false; // c2me 不兼容 28/8/28 打包，自动回落到 vanilla
		}
		return get().encoding.extendSectionEncoding;
	}

	public static boolean isFixChunkBoundaryGeneration() {
		return get().stability.fixChunkBoundaryGeneration;
	}

	public static boolean isGuardHugeMoveDelta() {
		return get().stability.guardHugeMoveDelta;
	}

	public static boolean isFixEntitySectionOverflow() {
		return get().stability.fixEntitySectionOverflow;
	}

	public static boolean isFixLightSectionCrash() {
		return get().stability.fixLightSectionCrash;
	}

	public static boolean isFixMineshaftOverflow() {
		return get().stability.fixMineshaftOverflow;
	}

	public static boolean isFixAquiferOverflow() {
		return get().stability.fixAquiferOverflow;
	}

	public static boolean isFixOctreeOverflow() {
		return get().stability.fixOctreeOverflow;
	}

	public static boolean isDisableStructuresFarOut() {
		return get().stability.disableStructuresFarOut;
	}
}
