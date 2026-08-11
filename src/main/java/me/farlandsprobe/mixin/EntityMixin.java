package me.farlandsprobe.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity#absSnapTo(double,double,double) clamps X/Z to +-30,000,000
 * (Mth.clamp(v, -3.0E7, 3.0E7)). ServerGamePacketListenerImpl calls
 * absSnapTo() after every player move packet, which pins the player back to
 * exactly 30,000,000 every tick — the "third wall" (and why the guard logs
 * show the server stuck at 3.0E7 while the client is at 33,554,432.5).
 * These redirects neutralize that clamp.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	@Redirect(
		method = "absSnapTo(DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 0)
	)
	private static double farlandsprobe$noAbsSnapClampX(double value, double min, double max) {
		return value;
	}

	@Redirect(
		method = "absSnapTo(DDD)V",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D", ordinal = 1)
	)
	private static double farlandsprobe$noAbsSnapClampZ(double value, double min, double max) {
		return value;
	}
}
