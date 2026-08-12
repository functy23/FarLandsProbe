package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 移除区块生成门禁:原版拒绝为超过 ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE
 * (约 ±33,553,360 格)的区块创建 holder,并在 GenerationChunkHolder 中抛错。
 * 我们放行生成,以便观察该上限之外的精度/打包损坏。
 * 当 {@link FarLandsProbeConfig#isAllowChunkGenerationEverywhere()} 关闭时本注入失效。
 */
@Mixin(ChunkPos.class)
public class ChunkPosMixin {
	@Inject(method = "isValid()Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$alwaysValid(CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isAllowChunkGenerationEverywhere()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isValid(II)Z", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$alwaysValidStatic(int x, int z, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isAllowChunkGenerationEverywhere()) {
			cir.setReturnValue(true);
		}
	}
}
