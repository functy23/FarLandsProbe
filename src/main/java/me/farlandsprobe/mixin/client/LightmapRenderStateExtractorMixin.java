package me.farlandsprobe.mixin.client;

import me.farlandsprobe.support.FullBrightSupport;
import me.farlandsprobe.support.client.LightmapFullBrightSupport;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.LightmapRenderStateExtractor;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 全亮:光图渲染状态提取完成后,把天空/方块因子强制为最大,清零黑暗效果,并把
 * gamma/亮度拉到上限。
 * 全亮关闭时本注入失效(见 {@link FullBrightSupport})。
 */
@Environment(EnvType.CLIENT)
@Mixin(LightmapRenderStateExtractor.class)
public class LightmapRenderStateExtractorMixin {
	@Inject(method = "extract(Lnet/minecraft/client/renderer/state/LightmapRenderState;F)V", at = @At("RETURN"))
	private void farlandsprobe$forceFullBright(LightmapRenderState renderState, float partialTicks, CallbackInfo ci) {
		if (!FullBrightSupport.isEnabled()) {
			return;
		}
		LightmapFullBrightSupport.applyToLightmap(renderState);
	}
}
