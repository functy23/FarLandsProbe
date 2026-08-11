package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.x vanilla hard-clamps the player's X/Z back to +-29,999,999 every tick
 * (Player#tick), which acts as an invisible wall at the old border position:
 * you cannot walk past it, and after /tp you get yanked back on the next tick.
 * These redirects neutralize that clamp (falling back to vanilla Mth.clamp when
 * {@link FarLandsProbeConfig#isDisableMovementClamps()} is off).
 *
 * Additionally, once the player passes the signed 32-bit int limit (+-2^31),
 * BlockPos (int) wraps around while the entity position (double) keeps going,
 * so the terrain generates 2^32 blocks away and is invisible. When
 * {@link FarLandsProbeConfig#isWrapCoordinates()} is on, the player position is
 * wrapped back by 2^32 to stay aligned with the int coordinate space, so chunk
 * generation/rendering continue (the world folds around).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)
	)
	private double farlandsprobe$noClampX(double value, double min, double max) {
		return FarLandsProbeConfig.isDisableMovementClamps() ? value : Mth.clamp(value, min, max);
	}

	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)
	)
	private double farlandsprobe$noClampZ(double value, double min, double max) {
		return FarLandsProbeConfig.isDisableMovementClamps() ? value : Mth.clamp(value, min, max);
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void farlandsprobe$wrapPastIntLimit(CallbackInfo ci) {
		if (!FarLandsProbeConfig.isWrapCoordinates()) {
			return;
		}
		Player self = (Player) (Object) this;
		double x = self.getX();
		double z = self.getZ();
		double dx = wrapDelta(x);
		double dz = wrapDelta(z);
		if (dx != 0.0 || dz != 0.0) {
			self.xo += dx;
			self.zo += dz;
			self.setPosRaw(x + dx, self.getY(), z + dz);
		}
	}

	/** Wraps a double coordinate into the signed 32-bit int range (-2^31 .. 2^31-1). */
	private static double wrapDelta(double v) {
		if (v >= -2147483648.0 && v <= 2147483647.0) {
			return 0.0;
		}
		double wrapped = v - 4294967296.0 * Math.floor((v + 2147483648.0) / 4294967296.0);
		return wrapped - v;
	}
}
