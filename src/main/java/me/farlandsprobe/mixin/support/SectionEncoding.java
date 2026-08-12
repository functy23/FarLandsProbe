package me.farlandsprobe.mixin.support;

/**
 * The 28/8/28 section-coordinate packing used by {@code SectionPosMixin}
 * (X/Z = 28 bits, Y = 8 bits; 28+8+28 = 64).
 *
 * Kept as a plain, Minecraft-free utility so the bit math can be unit-tested
 * without booting the game. See SectionPosMixin for the rationale behind the
 * 28/8/28 layout (extending X/Z to the int32 edge while Y only needs the
 * world-height range).
 */
public final class SectionEncoding {
	public static final int PACKED_X_LENGTH = 28;
	public static final int PACKED_Y_LENGTH = 8;
	public static final int PACKED_Z_LENGTH = 28;
	public static final int Y_OFFSET = 0;
	public static final int Z_OFFSET = PACKED_Y_LENGTH;
	public static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;
	private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
	private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
	private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;

	private SectionEncoding() {
	}

	public static long asLong(int x, int y, int z) {
		long node = 0L;
		node |= (x & PACKED_X_MASK) << X_OFFSET;
		node |= (y & PACKED_Y_MASK) << Y_OFFSET;
		node |= (z & PACKED_Z_MASK) << Z_OFFSET;
		return node;
	}

	public static int x(long sectionNode) {
		return (int) (sectionNode << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH);
	}

	public static int y(long sectionNode) {
		return (int) (sectionNode << 64 - Y_OFFSET - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH);
	}

	public static int z(long sectionNode) {
		return (int) (sectionNode << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH);
	}

	/** Clears the Y field only. */
	public static long getZeroNode(long sectionNode) {
		return sectionNode & -(1L << PACKED_Y_LENGTH);
	}
}
