package me.farlandsprobe.support;

import me.farlandsprobe.config.FarLandsProbeConfig;

/**
 * Central place for the fullbright feature: every fullbright mixin asks
 * {@link #isEnabled()} and uses {@link #MAX_LIGHT}, so changing the feature
 * (toggle name, max light value) touches one class instead of four mixins.
 */
public final class FullBrightSupport {
	/** Maximum light level forced everywhere when fullbright is on. */
	public static final int MAX_LIGHT = 15;

	private FullBrightSupport() {
	}

	public static boolean isEnabled() {
		return FarLandsProbeConfig.isFullBright();
	}
}
