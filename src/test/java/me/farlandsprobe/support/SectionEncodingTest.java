package me.farlandsprobe.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SectionEncodingTest {
	private static final int MAX_XZ_SECTION = (1 << 27) - 1; // 28 位有符号最大值
	private static final int MIN_XZ_SECTION = -(1 << 27); // 28 位有符号最小值
	private static final int MAX_Y_SECTION = (1 << 7) - 1; // 8 位有符号最大值
	private static final int MIN_Y_SECTION = -(1 << 7); // 8 位有符号最小值

	@Test
	void roundTripsNormalCoords() {
		long node = SectionEncoding.asLong(123, 4, -456);
		assertEquals(123, SectionEncoding.x(node));
		assertEquals(4, SectionEncoding.y(node));
		assertEquals(-456, SectionEncoding.z(node));
	}

	@Test
	void roundTripsAtXZEdge() {
		for (int x : new int[]{MAX_XZ_SECTION, MIN_XZ_SECTION, 0}) {
			for (int z : new int[]{MAX_XZ_SECTION, MIN_XZ_SECTION, 0}) {
				long node = SectionEncoding.asLong(x, 3, z);
				assertEquals(x, SectionEncoding.x(node), "x=" + x);
				assertEquals(z, SectionEncoding.z(node), "z=" + z);
			}
		}
	}

	@Test
	void roundTripsAtYEdge() {
		for (int y : new int[]{MAX_Y_SECTION, MIN_Y_SECTION, 0}) {
			long node = SectionEncoding.asLong(7, y, 9);
			assertEquals(y, SectionEncoding.y(node), "y=" + y);
		}
	}

	@Test
	void getZeroNodeClearsOnlyY() {
		long node = SectionEncoding.asLong(100, 42, 200);
		long zero = SectionEncoding.getZeroNode(node);
		assertEquals(0, SectionEncoding.y(zero));
		assertEquals(100, SectionEncoding.x(zero));
		assertEquals(200, SectionEncoding.z(zero));
	}

	@Test
	void zeroNodeStaysEdgeSafe() {
		long node = SectionEncoding.asLong(MAX_XZ_SECTION, MIN_Y_SECTION, MIN_XZ_SECTION);
		long zero = SectionEncoding.getZeroNode(node);
		assertEquals(MAX_XZ_SECTION, SectionEncoding.x(zero));
		assertEquals(MIN_XZ_SECTION, SectionEncoding.z(zero));
	}

	@Test
	void roundTripsAllAxesExtremeTogether() {
		long node = SectionEncoding.asLong(MAX_XZ_SECTION, MIN_Y_SECTION, MIN_XZ_SECTION);
		assertEquals(MAX_XZ_SECTION, SectionEncoding.x(node));
		assertEquals(MIN_Y_SECTION, SectionEncoding.y(node));
		assertEquals(MIN_XZ_SECTION, SectionEncoding.z(node));
	}
}
