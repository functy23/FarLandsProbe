package me.farlandsprobe.mixin.support.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.state.LightmapRenderState;

/**
 * Client-side half of fullbright: applies the maxed lightmap values to a
 * {@link LightmapRenderState}. Kept out of {@code FullBrightSupport} (which is
 * shared with the dedicated server) so the common class never references a
 * client-only type.
 */
@Environment(EnvType.CLIENT)
public final class LightmapFullBrightSupport {
	private LightmapFullBrightSupport() {
	}

	public static void applyToLightmap(LightmapRenderState renderState) {
		renderState.skyFactor = 15.0F;
		renderState.blockFactor = 15.0F;
		renderState.darknessEffectScale = 0.0F;
		renderState.brightness = 1.0F;
	}
}
