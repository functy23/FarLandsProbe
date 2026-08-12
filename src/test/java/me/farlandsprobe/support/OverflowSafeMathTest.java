package me.farlandsprobe.support;

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
		// 原版 (a+b)/2 在这里会溢出
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
		// 方块坐标 2^30 附近,与远地废弃矿井走廊的情况一致
		int x0 = 1073741824; // 2^30
		int x1 = 1073741834;
		assertEquals(1073741829, OverflowSafeMath.midpoint(x0, x1));
	}

	@Test
	void midpointAcrossFullIntRange() {
		// (a & b) + ((a ^ b) >> 1) 必须能处理 b - a 回绕整个 int 范围的情况。
		assertEquals(-1, OverflowSafeMath.midpoint(Integer.MIN_VALUE, Integer.MAX_VALUE));
		assertEquals(0, OverflowSafeMath.midpoint(-1, 1));
	}
}
