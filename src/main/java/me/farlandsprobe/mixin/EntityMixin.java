package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.MovementClampSupport;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity#absSnapTo(double,double,double) 会把 X/Z 夹到 ±30,000,000
 * (Mth.clamp(v, -3.0E7, 3.0E7))。ServerGamePacketListenerImpl 在每个玩家移动包
 * 之后都调用 absSnapTo(),于是玩家每 tick 都被钉回恰好 30,000,000——这就是
 * "第三堵墙"(也解释了为什么防护日志里服务器卡在 3.0E7 而客户端已在
 * 33,554,432.5)。这些重定向中和该夹取(当
 * {@link FarLandsProbeConfig#isDisableMovementClamps()} 关闭时回退到原版)。
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Redirect(
		method = "absSnapTo(DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)
	)
	private static double farlandsprobe$noAbsSnapClampX(double value, double min, double max) {
		return MovementClampSupport.clampIfEnabled(value, min, max);
	}

	@Redirect(
		method = "absSnapTo(DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)
	)
	private static double farlandsprobe$noAbsSnapClampZ(double value, double min, double max) {
		return MovementClampSupport.clampIfEnabled(value, min, max);
	}
}
