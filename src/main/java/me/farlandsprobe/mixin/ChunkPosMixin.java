package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Chunk generation gate removal: vanilla refuses to create chunk holders past
 * ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE (~ +-33,553,360 blocks) and throws
 * in GenerationChunkHolder. We let generation continue so the precision/packing
 * corruption beyond that limit can be observed.
 * Disabled when {@link FarLandsProbeConfig#isAllowChunkGenerationEverywhere()} is off.
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
