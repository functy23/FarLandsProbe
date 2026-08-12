package me.farlandsprobe.mixin;

import me.farlandsprobe.support.FullBrightSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全亮:原始亮度查询(作物生长、怪物生成判定、寻路、调试覆盖层)恒为 15。
 * 全亮关闭时本注入失效(回退到原版;见 {@link FullBrightSupport})。
 */
@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngineMixin {
	@Inject(method = "getRawBrightness(Lnet/minecraft/core/BlockPos;I)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$forceRawBrightness(BlockPos pos, int skyDampen, CallbackInfoReturnable<Integer> cir) {
		if (!FullBrightSupport.isEnabled()) {
			return;
		}
		cir.setReturnValue(FullBrightSupport.MAX_LIGHT);
	}
}
