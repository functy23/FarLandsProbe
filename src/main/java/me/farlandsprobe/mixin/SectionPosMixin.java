package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla packs section coordinates into a long with X/Z = 22 bits and Y = 20
 * bits, so every section-based system wraps at +-33,554,432 blocks for X/Z and
 * Y is limited to the 20-bit section range.
 *
 * This repack uses **X = 22 bits, Y = 22 bits, Z = 20 bits** (22+22+20 = 64):
 * - X stays at the vanilla ±33,554,432-block range (the far-lands direction),
 * - Y is extended to ±33,554,432 blocks — enough to reach the "sky far lands"
 *   around ~25,101,647 blocks with room to spare,
 * - Z is reduced to ±8,388,608 blocks to keep the total at 64 bits.
 *
 * Disabled when {@link FarLandsProbeConfig#isExtendSectionEncoding()} is off;
 * every injection then falls through to the vanilla 22/20/22 packing.
 */
@Mixin(SectionPos.class)
public abstract class SectionPosMixin {
	private static final int PACKED_X_LENGTH = 22;
	private static final int PACKED_Y_LENGTH = 22;
	private static final int PACKED_Z_LENGTH = 20;
	private static final long PACKED_X_MASK = (1L << PACKED_X_LENGTH) - 1L;
	private static final long PACKED_Y_MASK = (1L << PACKED_Y_LENGTH) - 1L;
	private static final long PACKED_Z_MASK = (1L << PACKED_Z_LENGTH) - 1L;
	private static final int Y_OFFSET = 0;
	private static final int Z_OFFSET = PACKED_Y_LENGTH;
	private static final int X_OFFSET = PACKED_Y_LENGTH + PACKED_Z_LENGTH;

	@Inject(method = "asLong(III)J", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedAsLong(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		long node = 0L;
		node |= (x & PACKED_X_MASK) << X_OFFSET;
		node |= (y & PACKED_Y_MASK) << Y_OFFSET;
		node |= (z & PACKED_Z_MASK) << Z_OFFSET;
		cir.setReturnValue(node);
	}

	@Inject(method = "x(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedX(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue((int) (sectionNode << 64 - X_OFFSET - PACKED_X_LENGTH >> 64 - PACKED_X_LENGTH));
	}

	@Inject(method = "y(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedY(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue((int) (sectionNode << 64 - Y_OFFSET - PACKED_Y_LENGTH >> 64 - PACKED_Y_LENGTH));
	}

	@Inject(method = "z(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedZ(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue((int) (sectionNode << 64 - Z_OFFSET - PACKED_Z_LENGTH >> 64 - PACKED_Z_LENGTH));
	}

	@Inject(method = "getZeroNode(J)J", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedZeroNode(long sectionNode, CallbackInfoReturnable<Long> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(sectionNode & -(1L << PACKED_Y_LENGTH));
	}
}
