package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.MovementClampSupport;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 26.x vanilla hard-clamps the player's X/Z back to +-29,999,999 every tick
 * (Player#tick), which acts as an invisible wall at the old border position:
 * you cannot walk past it, and after /tp you get yanked back on the next tick.
 * These redirects neutralize that clamp (falling back to vanilla Mth.clamp when
 * {@link FarLandsProbeConfig#isDisableMovementClamps()} is off).
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
