package me.farlandsprobe.support;

/**
 * 供远边界补丁使用的防溢出算术小工具。
 * 纯 Java、不依赖 Minecraft,可直接单元测试。
 */
public final class OverflowSafeMath {
	private OverflowSafeMath() {
	}

	/**
	 * 两个 int 的无溢出平均值(Hacker's Delight):
	 * {@code (a & b) + ((a ^ b) >> 1)}。
	 *
	 * 原版代码常写 {@code (a + b) / 2},当 {@code a + b} 超过 {@link Integer#MAX_VALUE}
	 * 时溢出;即使换成 {@code a + (b - a) / 2},当 {@code b - a} 本身回绕时(如废弃矿井
	 * 走廊在方块坐标 2^30 附近放置)仍会溢出。本形式对任意 int 输入都不会溢出,
	 * 包括全范围组合 ({@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}) → -1。
	 */
	public static int midpoint(int a, int b) {
		return (a & b) + ((a ^ b) >> 1);
	}
}
