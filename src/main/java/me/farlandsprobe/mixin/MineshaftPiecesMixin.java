package me.farlandsprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * MineshaftPieces$MineShaftPiece#isInInvalidLocation computes the corridor
 * midpoint with (x0 + x1) / 2. Past block coordinate 2^30 the sum overflows
 * int, the midpoint jumps to about -2^30, and the biome query asks for a chunk
 * 2^31 blocks away -> "Requested chunk unavailable during world generation".
 * Rewritten with the overflow-safe form x0 + (x1 - x0) / 2.
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces$MineShaftPiece")
public abstract class MineshaftPiecesMixin {
	/**
	 * @author farlandsprobe
	 * @reason Avoid int overflow in the corridor midpoint at extreme coordinates.
	 */
	@Overwrite
	protected boolean isInInvalidLocation(LevelAccessor level, BoundingBox chunkBB) {
		int x0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minX() - 1, chunkBB.minX());
		int y0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minY() - 1, chunkBB.minY());
		int z0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minZ() - 1, chunkBB.minZ());
		int x1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxX() + 1, chunkBB.maxX());
		int y1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxY() + 1, chunkBB.maxY());
		int z1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxZ() + 1, chunkBB.maxZ());
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(x0 + (x1 - x0) / 2, y0 + (y1 - y0) / 2, z0 + (z1 - z0) / 2);
		if (level.getBiome(blockPos).is(BiomeTags.MINESHAFT_BLOCKING)) {
			return true;
		}

		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (level.getBlockState(blockPos.set(x, y0, z)).liquid()) {
					return true;
				}

				if (level.getBlockState(blockPos.set(x, y1, z)).liquid()) {
					return true;
				}
			}
		}

		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x, y, z0)).liquid()) {
					return true;
				}

				if (level.getBlockState(blockPos.set(x, y, z1)).liquid()) {
					return true;
				}
			}
		}

		for (int z = z0; z <= z1; z++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x0, y, z)).liquid()) {
					return true;
				}

				if (level.getBlockState(blockPos.set(x1, y, z)).liquid()) {
					return true;
				}
			}
		}

		return false;
	}
}
