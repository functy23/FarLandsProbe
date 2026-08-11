package me.farlandsprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Bounds relaxation: allow spawning, teleporting, block interaction and height
 * queries anywhere, including far beyond the vanilla +-30,000,000 block limit.
 */
@Mixin(Level.class)
public abstract class LevelMixin {
	/**
	 * @author farlandsprobe
	 * @reason Remove the +-30M horizontal / +-20M vertical spawnability gate.
	 */
	@Overwrite
	public static boolean isInSpawnableBounds(BlockPos pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Accept positions anywhere (used by command arguments etc).
	 */
	@Overwrite
	public boolean isInWorldBounds(BlockPos pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Accept positions anywhere.
	 */
	@Overwrite
	public boolean isInValidBounds(BlockPos pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the +-30M branch in height queries so far-lands terrain
	 * heights are actually queried from generated chunks.
	 */
	@Overwrite
	public int getHeight(Heightmap.Types type, int x, int z) {
		Level self = (Level) (Object) this;
		if (self.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
			return self.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(type, x & 15, z & 15) + 1;
		}
		return self.getMinY();
	}
}
