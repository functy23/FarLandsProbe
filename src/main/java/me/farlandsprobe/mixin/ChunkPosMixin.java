package me.farlandsprobe.mixin;

import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Chunk generation gate removal: vanilla refuses to create chunk holders past
 * ChunkPyramid.MAX_CHUNK_COORDINATE_VALUE (~ +-33,553,360 blocks) and throws
 * in GenerationChunkHolder. We let generation continue so the precision/packing
 * corruption beyond that limit can be observed.
 */
@Mixin(ChunkPos.class)
public class ChunkPosMixin {
	/**
	 * @author farlandsprobe
	 * @reason Allow chunk generation / holders at any coordinates.
	 */
	@Overwrite
	public boolean isValid() {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Allow chunk generation / holders at any coordinates.
	 */
	@Overwrite
	public static boolean isValid(int x, int z) {
		return true;
	}
}
