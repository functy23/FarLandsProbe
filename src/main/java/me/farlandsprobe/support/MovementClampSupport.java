package me.farlandsprobe.support;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.util.Mth;

/**
 * Shared logic for the movement-clamp redirects in {@code PlayerMixin} and
 * {@code EntityMixin}: when {@code disableMovementClamps} is on, the value is
 * returned untouched; otherwise it falls back to vanilla {@link Mth#clamp}.
 *
 * Centralizing this means the "invisible wall" removal behaves identically at
 * every injection point, and changing the policy touches one class.
 */
public final class MovementClampSupport {
	private MovementClampSupport() {
	}

	public static double clampIfEnabled(double value, double min, double max) {
		return FarLandsProbeConfig.isDisableMovementClamps() ? value : Mth.clamp(value, min, max);
	}
}
