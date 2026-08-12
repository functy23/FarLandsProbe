package me.farlandsprobe.mixin;

import me.farlandsprobe.support.FullBrightSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.LightLayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 全亮:所有方块/实体顶点光强制为 15(最大值)。
 * 这是方块模型(LightCoordsUtil.BrightnessGetter)与实体渲染
 * (EntityRenderer#getSkyLight/getBlockLight)共用的唯一汇聚点。
 * 全亮关闭时本注入失效(见 {@link FullBrightSupport})。
 */
@Mixin(BlockAndLightGetter.class)
public interface BlockAndLightGetterMixin {
	@Inject(method = "getBrightness(Lnet/minecraft/world/level/LightLayer;Lnet/minecraft/core/BlockPos;)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$forceMaxBrightness(LightLayer layer, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
		if (!FullBrightSupport.isEnabled()) {
			return;
		}
		cir.setReturnValue(FullBrightSupport.MAX_LIGHT);
	}
}
