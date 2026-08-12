package me.farlandsprobe.support;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.util.Mth;

/**
 * {@code PlayerMixin} 与 {@code EntityMixin} 中移动夹取重定向的共享逻辑:
 * 当 {@code disableMovementClamps} 开启时原样返回值;否则回退到原版 {@link Mth#clamp}。
 *
 * 集中到这里,使"隐形墙"移除在每个注入点行为一致,改策略只需动这一个类。
 */
public final class MovementClampSupport {
	private MovementClampSupport() {
	}

	public static double clampIfEnabled(double value, double min, double max) {
		return FarLandsProbeConfig.isDisableMovementClamps() ? value : Mth.clamp(value, min, max);
	}
}
