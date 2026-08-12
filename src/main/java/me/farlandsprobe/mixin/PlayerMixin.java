package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.MovementClampSupport;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.x 原版每 tick 都会把玩家的 X/Z 硬夹回 ±29,999,999(Player#tick),这在旧边界
 * 位置形成了一堵隐形墙:你走不过去,而且 /tp 之后下一 tick 会被拉回来。
 * 这些重定向中和该夹取(当 {@link FarLandsProbeConfig#isDisableMovementClamps()}
 * 关闭时回退到原版 Mth.clamp)。
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)
	)
	private double farlandsprobe$noClampX(double value, double min, double max) {
		return MovementClampSupport.clampIfEnabled(value, min, max);
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)
	)
	private double farlandsprobe$noClampZ(double value, double min, double max) {
		return MovementClampSupport.clampIfEnabled(value, min, max);
	}
}
