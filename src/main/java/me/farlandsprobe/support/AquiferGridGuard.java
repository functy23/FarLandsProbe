package me.farlandsprobe.support;

/**
 * 含水层(Aquifer)网格尺寸的防溢出判定。
 *
 * 原版在构造含水层时用全 int 运算推导网格边长:
 * <pre>
 *   minGridX = gridX(minBlockX - 5);  maxGridX = gridX(maxBlockX - 5) + 1;
 *   gridSizeX = maxGridX - minGridX + 1;   // X/Z 用 &gt;&gt; 4,Y 用 floorDiv(v, 12)
 *   ...
 *   aquiferCache = new FluidStatus[gridSizeX * gridSizeY * gridSizeZ];
 * </pre>
 * 在接近 ±2^31 方块上限处,{@code minBlockX - 5} 与三边连乘都会溢出成巨大的正数,
 * 于是数组分配耗尽堆内存(世界生成期间的 OutOfMemoryError)。
 *
 * 这里用 long 复算同一套公式,在"块坐标贴着 int 边界"或"网格荒谬地大"时返回 true,
 * 让调用方改用 {@code Aquifer.createDisabled(...)} 而不是分配内存。
 *
 * 纯 Java、不依赖 Minecraft,可直接单元测试。
 * 26.1.2 / 26.2 由 {@code AquiferMixin} 使用,26.3 由 {@code AquiferConfigMixin} 使用
 * (两版的网格公式一致,只是入口从 {@code Aquifer.create} 搬到了 {@code Aquifer$Config.create})。
 */
public final class AquiferGridGuard {
	/** 块坐标距 int 边界的安全裕量:小于它时原版的 {@code -5} 偏移就可能溢出。 */
	public static final long INT_EDGE_SAFE_LIMIT = 2_000_000_000L;
	/** 正常区块的网格总量约 2×35×2 = 140;超过这个数量级一定是坐标溢出。 */
	public static final long MAX_AQUIFER_GRID = 4_000_000L;
	/** 单边上限,防止"总量看着正常但某一边爆炸"的情况。 */
	public static final long MAX_GRID_SIDE = 4096L;

	/** 原版 {@code NoiseBasedAquifer} 的采样间距:X/Z 每 16 格、Y 每 12 格取一个网格点。 */
	private static final int XZ_GRID_SHIFT = 4;
	private static final int Y_GRID_SPAN = 12;
	/** 原版给区块包围盒加的 ±5 格采样外扩。 */
	private static final int SAMPLE_MARGIN = 5;

	private AquiferGridGuard() {
	}

	/**
	 * 是否应当放弃含水层(改用 {@code createDisabled})。
	 *
	 * @param minBlockX 体积/区块的最小方块 X(可为 int 边界附近的值)
	 * @param maxBlockX 体积/区块的最大方块 X
	 * @param minBlockY 最小方块 Y
	 * @param maxBlockY 最大方块 Y
	 * @param minBlockZ 最小方块 Z
	 * @param maxBlockZ 最大方块 Z
	 */
	public static boolean shouldDisableAquifer(
		long minBlockX, long maxBlockX, long minBlockY, long maxBlockY, long minBlockZ, long maxBlockZ
	) {
		// 块坐标已贴着 int 边界:原版的 -5 / +1 偏移与 int 连乘必然溢出。
		if (minBlockX < -INT_EDGE_SAFE_LIMIT || maxBlockX > INT_EDGE_SAFE_LIMIT
			|| minBlockZ < -INT_EDGE_SAFE_LIMIT || maxBlockZ > INT_EDGE_SAFE_LIMIT) {
			return true;
		}

		// 与原版 NoiseBasedAquifer 构造器逐行对应,只是全部用 long 复算。
		long minGridX = gridX(minBlockX - SAMPLE_MARGIN);
		long maxGridX = gridX(maxBlockX - SAMPLE_MARGIN) + 1;
		long gridSizeX = maxGridX - minGridX + 1;

		long minGridY = gridY(minBlockY + 1) - 1;
		long maxGridY = gridY(maxBlockY + 1) + 1;
		long gridSizeY = maxGridY - minGridY + 1;

		long minGridZ = gridZ(minBlockZ - SAMPLE_MARGIN);
		long maxGridZ = gridZ(maxBlockZ - SAMPLE_MARGIN) + 1;
		long gridSizeZ = maxGridZ - minGridZ + 1;

		long total = gridSizeX * gridSizeY * gridSizeZ;
		return total <= 0
			|| total > MAX_AQUIFER_GRID
			|| gridSizeX > MAX_GRID_SIDE
			|| gridSizeY > MAX_GRID_SIDE
			|| gridSizeZ > MAX_GRID_SIDE;
	}

	/** 原版 {@code NoiseBasedAquifer.gridX/gridZ}:{@code v >> 4}(对负数同样是向下取整)。 */
	private static long gridX(long blockCoord) {
		return blockCoord >> XZ_GRID_SHIFT;
	}

	private static long gridZ(long blockCoord) {
		return blockCoord >> XZ_GRID_SHIFT;
	}

	/** 原版 {@code NoiseBasedAquifer.gridY}:{@code Math.floorDiv(v, 12)}。 */
	private static long gridY(long blockCoord) {
		return Math.floorDiv(blockCoord, (long) Y_GRID_SPAN);
	}
}
