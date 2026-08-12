package me.farlandsprobe.mixin.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OverflowSafeMathTest {
	@Test
	void normalMidpoint() {
		assertEquals(5, OverflowSafeMath.midpoint(0, 10));
		assertEquals(-5, OverflowSafeMath.midpoint(-10, 0));
	}

	@Test
	void midpointAtIntMaxDoesNotOverflow() {
		// vanilla (a+b)/2 would overflow here
		int a = Integer.MAX_VALUE - 1;
		int b = Integer.MAX_VALUE;
		assertEquals(Integer.MAX_VALUE - 1, OverflowSafeMath.midpoint(a, b));
	}

	@Test
	void midpointAtIntMinDoesNotOverflow() {
		int a = Integer.MIN_VALUE;
		int b = Integer.MIN_VALUE + 10;
		assertEquals(Integer.MIN_VALUE + 5, OverflowSafeMath.midpoint(a, b));
	}

	@Test
	void mineshaftCorridorScenario() {
		// near block coordinate 2^30, like mineshaft corridors in the far lands
		int x0 = 1073741824; // 2^30
		int x1 = 1073741834;
		assertEquals(1073741829, OverflowSafeMath.midpoint(x0, x1));
	}

	@Test
	void midpointAcrossFullIntRange() {
		// (a & b) + ((a ^ b) >> 1) must handle b - a wrapping the full int range.
		assertEquals(-1, OverflowSafeMath.midpoint(Integer.MIN_VALUE, Integer.MAX_VALUE));
		assertEquals(0, OverflowSafeMath.midpoint(-1, 1));
	}
}
