package me.farlandsprobe.support;

import me.farlandsprobe.config.FarLandsProbeConfig;

/**
 * 全亮功能的集中点:每个全亮 mixin 都查询 {@link #isEnabled()} 并使用
 * {@link #MAX_LIGHT},因此改功能(开关名、最大亮度值)只动这一个类,而不是四个 mixin。
 */
public final class FullBrightSupport {
	/** 全亮开启时全局强制的最大亮度值。 */
	public static final int MAX_LIGHT = 15;

	private FullBrightSupport() {
	}

	public static boolean isEnabled() {
		return FarLandsProbeConfig.isFullBright();
	}
}
