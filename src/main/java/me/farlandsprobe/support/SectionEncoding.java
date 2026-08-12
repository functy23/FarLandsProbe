package me.farlandsprobe.support;

/**
 * {@code SectionPosMixin} 使用的 28/8/28 section 坐标打包(X/Z = 28 位,Y = 8 位;28+8+28 = 64)。
 *
 * 保持为纯 Java、不依赖 Minecraft 的工具类,位运算可以直接做单元测试而无需启动游戏。
 * 28/8/28 布局的理由见 SectionPosMixin:把 X/Z 扩展到 int32 边界,而 Y 只需要世界高度范围。
 */
public final class SectionEncoding {
	public static final int PACKED_X_LENGTH = 28;
	public static final int PACKED_Y_LENGTH = 8;
	public static final int PACKED_Z_LENGTH = 28;
	public static final int Y_OFFSET = 0;
	public static final int Z_OFFSET = PACKED_Y_LENGTH;
	public static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

	/** section 坐标回绕周期 = 2^28:X/Z 字段溢出后按该周期回绕。边界处理见 WorldGenRegionMixin / StaticCache2DMixin。 */
	public static final long WRAP_PERIOD = 1L << PACKED_X_LENGTH;

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

	/** 只清零 Y 字段。 */
	public static long getZeroNode(long sectionNode) {
		return sectionNode & -(1L << PACKED_Y_LENGTH);
	}
}
