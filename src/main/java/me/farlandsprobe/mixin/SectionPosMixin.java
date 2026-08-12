package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.mixin.support.SectionEncoding;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla packs section coordinates into a long with X/Z = 22 bits and Y = 20
 * bits, so every section-based system (chunk renderer SectionOcclusionGraph,
 * ClientChunkCache, lighting storage, entity sections...) wraps at
 * +-33,554,432 blocks -> terrain beyond that never renders/generates.
 *
 * We repack with X/Z = 28 bits and Y = 8 bits (world height is only 4064
 * blocks = 254 sections, so 8 bits suffice). The render/generation limit moves
 * to +-2,147,483,632 blocks; BlockPos block-node packing (26-bit) is left
 * untouched so its overflow at 33,554,432 produces the visible corruption the
 * mod is meant to explore.
 *
 * The bit math lives in {@link SectionEncoding} so it can be unit-tested.
 * Disabled when {@link FarLandsProbeConfig#isExtendSectionEncoding()} is off;
 * every injection then falls through to the vanilla 22/20/22 packing.
 */
@Mixin(SectionPos.class)
public abstract class SectionPosMixin {
	@Inject(method = "asLong(III)J", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedAsLong(int x, int y, int z, CallbackInfoReturnable<Long> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(SectionEncoding.asLong(x, y, z));
	}

	@Inject(method = "x(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedX(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(SectionEncoding.x(sectionNode));
	}

	@Inject(method = "y(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedY(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(SectionEncoding.y(sectionNode));
	}

	@Inject(method = "z(J)I", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedZ(long sectionNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(SectionEncoding.z(sectionNode));
	}

	@Inject(method = "getZeroNode(J)J", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$extendedZeroNode(long sectionNode, CallbackInfoReturnable<Long> cir) {
		if (!FarLandsProbeConfig.isExtendSectionEncoding()) {
			return;
		}
		cir.setReturnValue(SectionEncoding.getZeroNode(sectionNode));
	}
}
