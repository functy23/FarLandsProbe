package me.farlandsprobe.mixin;

import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Near the +-2^31 block limit, mineshaft piece coordinates can overflow int and
 * feed garbage coordinates into NoiseChunk/Aquifer. The aquifer grid size then
 * becomes astronomically large and `new long[totalGridSize]` exhausts the heap
 * (OutOfMemoryError during worldgen). Guard: if the grid is absurd, disable the
 * aquifer instead of allocating.
 */
@Mixin(Aquifer.class)
public interface AquiferMixin {
	@Inject(
		method = "create(Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/NoiseRouter;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;IILnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void farlandsprobe$guardAbsurdAquiferGrid(
		NoiseChunk noiseChunk,
		ChunkPos pos,
		NoiseRouter router,
		PositionalRandomFactory positionalRandomFactory,
		int minBlockY,
		int yBlockSize,
		Aquifer.FluidPicker fluidRule,
		CallbackInfoReturnable<Aquifer> cir
	) {
		int gridSizeX = ((pos.getMaxBlockX() - 5) >> 4) + 1 - ((pos.getMinBlockX() - 5) >> 4) + 1;
		int gridSizeY = Math.floorDiv(minBlockY + yBlockSize + 1, 12) + 1 - (Math.floorDiv(minBlockY + 1, 12) - 1) + 1;
		int gridSizeZ = ((pos.getMaxBlockZ() - 5) >> 4) + 1 - ((pos.getMinBlockZ() - 5) >> 4) + 1;
		long total = (long) gridSizeX * gridSizeY * gridSizeZ;
		if (total > 4_000_000L || gridSizeX > 4096 || gridSizeY > 4096 || gridSizeZ > 4096) {
			LoggerFactory.getLogger("farlandsprobe").warn(
				"[farlandsprobe] absurd aquifer grid {}x{}x{}={} at pos={} minY={} height={}; disabling aquifer",
				gridSizeX, gridSizeY, gridSizeZ, total, pos, minBlockY, yBlockSize
			);
			cir.setReturnValue(Aquifer.createDisabled(fluidRule));
		}
	}
}
