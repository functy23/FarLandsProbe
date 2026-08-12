package me.farlandsprobe.support;

/**
 * Small overflow-safe arithmetic helpers used by the far-edge patches.
 * Plain, Minecraft-free, unit-testable.
 */
public final class OverflowSafeMath {
	private OverflowSafeMath() {
	}

	/**
	 * Overflow-free average of two ints (Hacker's Delight):
	 * {@code (a & b) + ((a ^ b) >> 1)}.
	 *
	 * Vanilla code often writes {@code (a + b) / 2}, which overflows when
	 * {@code a + b} exceeds {@link Integer#MAX_VALUE}; even {@code a + (b - a) / 2}
	 * still overflows when {@code b - a} itself wraps (e.g. mineshaft corridor
	 * placement near block coordinate 2^30). This form never overflows for any
	 * int inputs, including the full-range pair
	 * ({@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}) → -1.
	 */
	public static int midpoint(int a, int b) {
		return (a & b) + ((a ^ b) >> 1);
	}
}
