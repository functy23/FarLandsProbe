package me.farlandsprobe.support.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.state.LightmapRenderState;

/**
 * 全亮的客户端半边:把拉满的光图值应用到 {@link LightmapRenderState}。
 * 放在 {@code FullBrightSupport}(与专用服务器共享)之外,公共类就不必引用
 * 仅客户端存在的类型。
 */
@Environment(EnvType.CLIENT)
public final class LightmapFullBrightSupport {
	private LightmapFullBrightSupport() {
	}

	public static void applyToLightmap(LightmapRenderState renderState) {
		// skyFactor/blockFactor 最大 15(MC 光照等级上限);brightness 归一化为 1.0;黑暗效果关闭为 0.0。
		renderState.skyFactor = 15.0F;
		renderState.blockFactor = 15.0F;
		renderState.darknessEffectScale = 0.0F;
		renderState.brightness = 1.0F;
	}
}
