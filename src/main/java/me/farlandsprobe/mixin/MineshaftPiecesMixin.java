package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.OverflowSafeMath;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineshaftPieces$MineShaftPiece#isInInvalidLocation computes the corridor
 * midpoint with (x0 + x1) / 2. Past block coordinate 2^30 the sum overflows
 * int, the midpoint jumps to about -2^30, and the biome query asks for a chunk
 * 2^31 blocks away -> "Requested chunk unavailable during world generation".
 * Rewritten with the overflow-safe form x0 + (x1 - x0) / 2.
 * Disabled when {@link FarLandsProbeConfig#isFixMineshaftOverflow()} is off.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces$MineShaftPiece")
public abstract class MineshaftPiecesMixin {
	// NOTE: this body is a 1:1 port of vanilla isInInvalidLocation, with only the
	// corridor midpoint rewritten overflow-safe. When syncing with an upstream
	// Minecraft update, diff against the vanilla method first.
	@Inject(method = "isInInvalidLocation(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$overflowSafeMidpoint(LevelAccessor level, BoundingBox chunkBB, CallbackInfoReturnable<Boolean> cir) {
		if (!FarLandsProbeConfig.isFixMineshaftOverflow()) {
			return;
		}
		int x0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minX() - 1, chunkBB.minX());
		int y0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minY() - 1, chunkBB.minY());
		int z0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minZ() - 1, chunkBB.minZ());
		int x1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxX() + 1, chunkBB.maxX());
		int y1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxY() + 1, chunkBB.maxY());
		int z1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxZ() + 1, chunkBB.maxZ());
		// Overflow-safe midpoint: vanilla (x0+x1)/2 overflows past 2^30; see OverflowSafeMath.
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(
			OverflowSafeMath.midpoint(x0, x1), OverflowSafeMath.midpoint(y0, y1), OverflowSafeMath.midpoint(z0, z1));
		if (level.getBiome(blockPos).is(BiomeTags.MINESHAFT_BLOCKING)) {
			cir.setReturnValue(true);
			return;
		}

		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (level.getBlockState(blockPos.set(x, y0, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x, y1, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x, y, z0)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x, y, z1)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		for (int z = z0; z <= z1; z++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x0, y, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x1, y, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		cir.setReturnValue(false);
	}
}
