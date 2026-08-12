package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
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
 * Near the +-2^31 block limit, garbage coordinates (e.g. from mineshaft piece
 * int overflow) can make the aquifer grid-size product absurdly large, so
 * `new long[totalGridSize]` exhausts the heap (OutOfMemoryError during
 * worldgen). Guard with long math: if the (absurd) grid is too large, disable
 * the aquifer instead of allocating.
 * Disabled when {@link FarLandsProbeConfig#isFixAquiferOverflow()} is off.
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
		if (!FarLandsProbeConfig.isFixAquiferOverflow()) {
			return;
		}

		// Empirical thresholds: a normal chunk's aquifer grid is far smaller; anything above
		// is treated as garbage coordinates (gridSizeX/Y/Z are cell counts).
		final long MAX_AQUIFER_GRID = 4_000_000L;
		final long MAX_GRID_SIDE = 4096L;

		long minBlockX = pos.getMinBlockX();
		long maxBlockX = pos.getMaxBlockX();
		long minBlockZ = pos.getMinBlockZ();
		long maxBlockZ = pos.getMaxBlockZ();

		// Mirror vanilla's grid-size computation with long math so the int
		// overflow that happens near the coordinate edge cannot slip through.
		// X/Z grid step = 16 blocks (>>4); Y grid step = 12 blocks (floorDiv).
		long maxGridX = (maxBlockX - 5) >> 4;
		long minGridX = (minBlockX - 5) >> 4;
		long gridSizeX = (maxGridX + 1) - minGridX + 1;
		long maxGridY = Math.floorDiv((long) minBlockY + yBlockSize + 1, 12) + 1;
		long minGridY = Math.floorDiv((long) minBlockY + 1, 12) - 1;
		long gridSizeY = maxGridY - minGridY + 1;
		long maxGridZ = (maxBlockZ - 5) >> 4;
		long minGridZ = (minBlockZ - 5) >> 4;
		long gridSizeZ = (maxGridZ + 1) - minGridZ + 1;
		long total = gridSizeX * gridSizeY * gridSizeZ;
		if (Math.abs(total) > MAX_AQUIFER_GRID || Math.abs(gridSizeX) > MAX_GRID_SIDE
			|| Math.abs(gridSizeY) > MAX_GRID_SIDE || Math.abs(gridSizeZ) > MAX_GRID_SIDE) {
			LoggerFactory.getLogger("farlandsprobe").warn(
				"[farlandsprobe] absurd aquifer grid {}x{}x{}={} at pos={} minY={} height={}; disabling aquifer",
				gridSizeX, gridSizeY, gridSizeZ, total, pos, minBlockY, yBlockSize
			);
			cir.setReturnValue(Aquifer.createDisabled(fluidRule));
		}
	}
}
