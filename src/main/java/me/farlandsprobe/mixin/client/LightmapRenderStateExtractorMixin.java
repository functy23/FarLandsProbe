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
 * Fullbright: after the lightmap render state is extracted, force sky/block
 * factors to maximum, zero out the darkness effect and max out gamma/brightness.
 * Disabled when fullbright is off (see {@link FullBrightSupport}).
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
