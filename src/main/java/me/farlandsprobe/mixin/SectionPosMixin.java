package me.farlandsprobe.mixin;

import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Vanilla packs section coordinates into a long with X/Z = 22 bits and Y = 20
 * bits, so every section-based system (chunk renderer SectionOcclusionGraph,
 * ClientChunkCache, lighting storage, entity sections...) wraps at
 * +-33,554,432 blocks -> terrain beyond that never renders/generates.
 *
 * We repack with X/Z = 28 bits and Y = 8 bits (world height is only 4064
 * blocks = 254 sections, so 8 bits suffice). The render/generation limit moves
 * to +-2,147,483,632 blocks; BlockPos block-node packing (26-bit) is left
 * untouched so its overflow at 33,554,432 produces the visible corruption the
 * mod is meant to explore.
 */
@Mixin(SectionPos.class)
public abstract class SectionPosMixin {
	private static final int PACKED_X_LENGTH = 28;
	private static final int PACKED_Y_LENGTH = 8;
	private static final int PACKED_Z_LENGTH = 28;
	private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
	private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
	private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
	private static final int Y_OFFSET = 0;
	private static final int Z_OFFSET = PACKED_Y_LENGTH;
	private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

	/**
	 * @author farlandsprobe
	 * @reason Extended section-node packing (28/8/28 instead of 22/20/22).
	 */
	@Overwrite
	public static long asLong(int x, int y, int z) {
		long node = 0L;
		node |= (x & PACKED_X_MASK) << X_OFFSET;
		node |= (y & PACKED_Y_MASK) << Y_OFFSET;
		return node | (z & PACKED_Z_MASK) << Z_OFFSET;
	}

	/**
	 * @author farlandsprobe
	 * @reason Extended section-node packing (28/8/28 instead of 22/20/22).
	 */
	@Overwrite
	public static int x(long sectionNode) {
		return (int)(sectionNode << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
	}

	/**
	 * @author farlandsprobe
	 * @reason Extended section-node packing (28/8/28 instead of 22/20/22).
	 */
	@Overwrite
	public static int y(long sectionNode) {
		return (int)(sectionNode << 64 - Y_OFFSET - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
	}

	/**
	 * @author farlandsprobe
	 * @reason Extended section-node packing (28/8/28 instead of 22/20/22).
	 */
	@Overwrite
	public static int z(long sectionNode) {
		return (int)(sectionNode << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
	}

	/**
	 * @author farlandsprobe
	 * @reason Clear only the (now 8-bit) Y field.
	 */
	@Overwrite
	public static long getZeroNode(long sectionNode) {
		return sectionNode & -(1L << PACKED_Y_LENGTH);
	}
}
