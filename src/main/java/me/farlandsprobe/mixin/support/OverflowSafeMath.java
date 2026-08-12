package me.farlandsprobe.mixin.support;

/**
 * Small overflow-safe arithmetic helpers used by the far-edge patches.
 * Plain, Minecraft-free, unit-testable.
 */
public final class OverflowSafeMath {
	private OverflowSafeMath() {
	}

	/**
	 * Overflow-safe midpoint of two ints: {@code a + (b - a) / 2}.
	 * Vanilla code often writes {@code (a + b) / 2}, which overflows when
	 * {@code a + b} exceeds {@link Integer#MAX_VALUE} (e.g. mineshaft corridor
	 * placement near block coordinate 2^30).
	 */
	public static int midpoint(int a, int b) {
		return a + (b - a) / 2;
	}
}
