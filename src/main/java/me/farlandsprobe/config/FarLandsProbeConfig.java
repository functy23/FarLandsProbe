package me.farlandsprobe.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * FarLandsProbe configuration, powered by Cloth Config's AutoConfig.
 *
 * Every feature is a boolean toggle. All defaults are {@code true}, which keeps
 * the historical "everything enabled" behaviour for existing users. The file is
 * stored at {@code config/farlandsprobe.json}.
 *
 * All fields are marked {@link ConfigEntry.Gui.RequiresRestart} because the
 * patches hook into worldgen / networking / renderer setup at startup; changing
 * them takes effect on the next game restart.
 */
@Config(name = "farlandsprobe")
public class FarLandsProbeConfig implements ConfigData {

	// --- 光照 / Lighting ---
	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Lighting lighting = new Lighting();

	// --- 世界边境 / World border ---
	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Border border = new Border();

	// --- 生成与传送边界 / Spawn & teleport bounds ---
	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Bounds bounds = new Bounds();

	// --- 坐标编码 / Coordinate encoding ---
	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Encoding encoding = new Encoding();

	// --- 远地稳定性修复 / Far-lands stability patches ---
	@ConfigEntry.Gui.CollapsibleObject(startExpanded = true)
	public Stability stability = new Stability();

	/** 光照 / Lighting */
	public static class Lighting {
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fullBright = true;
	}

	/** 世界边境 / World border */
	public static class Border {
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean removeWorldBorder = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean disableMovementClamps = true;
	}

	/** 生成与传送边界 / Spawn & teleport bounds */
	public static class Bounds {
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean relaxSpawnAndTeleportBounds = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean allowChunkGenerationEverywhere = true;
	}

	/** 坐标编码 / Coordinate encoding */
	public static class Encoding {
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean extendSectionEncoding = true;
	}

	/** 远地稳定性修复 / Far-lands stability patches */
	public static class Stability {
		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixChunkBoundaryGeneration = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean guardHugeMoveDelta = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixEntitySectionOverflow = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixLightSectionCrash = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixMineshaftOverflow = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixAquiferOverflow = true;

		@ConfigEntry.Gui.Tooltip
		@ConfigEntry.Gui.RequiresRestart
		public boolean fixOctreeOverflow = true;

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
	 * C²M 引擎重写了区块存储/异步加载系统，基于 vanilla 22/20/22 的 section 打包，
	 * 与 28/8/28 坐标扩展不兼容（遇到 int 边界回绕区块会 "Max retries reached" 崩溃）。
	 * 检测到 c2me 时自动回落到 vanilla 编码。
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

	// --- 静态访问器（供 mixin 使用）/ static accessors used by the mixins ---

	// 光照 / Lighting
	public static boolean isFullBright() {
		return get().lighting.fullBright;
	}

	// 世界边境 / World border
	public static boolean isRemoveWorldBorder() {
		return get().border.removeWorldBorder;
	}

	public static boolean isDisableMovementClamps() {
		return get().border.disableMovementClamps;
	}

	// 生成与传送边界 / Spawn & teleport bounds
	public static boolean isRelaxSpawnAndTeleportBounds() {
		return get().bounds.relaxSpawnAndTeleportBounds;
	}

	public static boolean isAllowChunkGenerationEverywhere() {
		return get().bounds.allowChunkGenerationEverywhere;
	}

	// 坐标编码 / Coordinate encoding
	public static boolean isExtendSectionEncoding() {
		if (isC2mePresent()) {
			return false; // c2me 不兼容 28/8/28 打包，自动回落到 vanilla
		}
		return get().encoding.extendSectionEncoding;
	}

	// 远地稳定性修复 / Far-lands stability patches
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
