package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: raw brightness queries (used by crop growth, mob spawning checks,
 * pathfinding, debug overlays) always report 15.
 * Disabled when {@link FarLandsProbeConfig#isFullBright()} is off (falls back to vanilla).
 */
@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngineMixin {
	@Inject(method = "getRawBrightness(Lnet/minecraft/core/BlockPos;I)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$forceRawBrightness(BlockPos pos, int skyDampen, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isFullBright()) {
			return;
		}
		cir.setReturnValue(15);
	}
}
