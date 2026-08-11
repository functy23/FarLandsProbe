package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: every block/entity vertex light is forced to 15 (max).
 * This is the single choke point used by block models (LightCoordsUtil.BrightnessGetter)
 * and entity rendering (EntityRenderer#getSkyLight/getBlockLight).
 * Disabled when {@link FarLandsProbeConfig#isFullBright()} is off.
 */
@Mixin(BlockAndLightGetter.class)
public interface BlockAndLightGetterMixin {
	@Inject(method = "getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$forceMaxBrightness(LightLayer layer, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isFullBright()) {
			return;
		}
		cir.setReturnValue(15);
	}
}
