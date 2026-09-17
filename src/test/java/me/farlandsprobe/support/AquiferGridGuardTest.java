package me.farlandsprobe.support;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AquiferGridGuardTest {
	/** 一个普通区块的体积:16x16 格、Y 覆盖世界高度(-64..320)。 */
	private static final long CHUNK_MIN_X = 0L;
	private static final long CHUNK_MAX_X = 15L;
	private static final long CHUNK_MIN_Y = -64L;
	private static final long CHUNK_MAX_Y = 319L;
	private static final long CHUNK_MIN_Z = 0L;
	private static final long CHUNK_MAX_Z = 15L;

	@Test
	void normalChunkIsNotDisabled() {
		assertFalse(AquiferGridGuard.shouldDisableAquifer(
			CHUNK_MIN_X, CHUNK_MAX_X, CHUNK_MIN_Y, CHUNK_MAX_Y, CHUNK_MIN_Z, CHUNK_MAX_Z));
	}

	@Test
	void normalChunkFarFromOriginIsNotDisabled() {
		// 距原点 100 万格:仍在安全裕量内,网格尺寸不变。
		long base = 1_000_000L;
		assertFalse(AquiferGridGuard.shouldDisableAquifer(
			base, base + 15, CHUNK_MIN_Y, CHUNK_MAX_Y, -base, -base + 15));
	}

	@Test
	void positiveIntEdgeIsDisabled() {
		long base = Integer.MAX_VALUE - 16L;
		assertTrue(AquiferGridGuard.shouldDisableAquifer(
			base, base + 15, CHUNK_MIN_Y, CHUNK_MAX_Y, 0L, 15L));
	}

	@Test
	void negativeIntEdgeIsDisabled() {
		long base = Integer.MIN_VALUE;
		assertTrue(AquiferGridGuard.shouldDisableAquifer(
			base, base + 15, CHUNK_MIN_Y, CHUNK_MAX_Y, 0L, 15L));
	}

	@Test
	void sampleMarginOverflowIsDisabled() {
		// minBlockX - 5 会越过 int 边界:即使坐标本身还"看着"安全也必须禁用。
		assertTrue(AquiferGridGuard.shouldDisableAquifer(
			Integer.MIN_VALUE + 2L, Integer.MIN_VALUE + 17L, CHUNK_MIN_Y, CHUNK_MAX_Y, 0L, 15L));
	}

	@Test
	void absurdlyWideVolumeIsDisabled() {
		// 体积横跨半个世界:网格连乘会到 2^30 量级。
		assertTrue(AquiferGridGuard.shouldDisableAquifer(
			-1_000_000_000L, 1_000_000_000L, CHUNK_MIN_Y, CHUNK_MAX_Y, 0L, 15L));
	}

	@Test
	void hugelyInvertedVolumeIsDisabled() {
		// max 远小于 min(坐标回绕后的产物):原版的减法给出巨大负边长,连乘不可信。
		assertTrue(AquiferGridGuard.shouldDisableAquifer(
			2_000_000_000L, -2_000_000_000L, CHUNK_MIN_Y, CHUNK_MAX_Y, CHUNK_MIN_Z, CHUNK_MAX_Z));
	}

	@Test
	void mildlyInvertedVolumeIsNotDisabled() {
		// 只差几格的倒置体积不会溢出(原版此时网格仍是 1x35x1),
		// 本守卫只负责防 OOM,不做输入校验 —— 不能误伤正常世界生成。
		assertFalse(AquiferGridGuard.shouldDisableAquifer(
			CHUNK_MAX_X, CHUNK_MIN_X, CHUNK_MIN_Y, CHUNK_MAX_Y, CHUNK_MIN_Z, CHUNK_MAX_Z));
	}

	@Test
	void intMinYAloneIsNotDisabled() {
		// Y 方向原版用的是 Math.floorDiv(v, 12),对 int 极值同样给出正常边长
		// (floorDiv 不会溢出),因此 Y 极端但 X/Z 正常时不应触发禁用。
		assertFalse(AquiferGridGuard.shouldDisableAquifer(
			0L, 15L, Integer.MIN_VALUE, Integer.MIN_VALUE + 383L, 0L, 15L));
	}
}
