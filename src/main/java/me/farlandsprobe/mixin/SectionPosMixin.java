package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.SectionEncoding;
import net.minecraft.core.SectionPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版把 section 坐标打包进 long:X/Z 各 22 位、Y 20 位,因此所有基于 section 的
 * 系统(区块渲染 SectionOcclusionGraph、ClientChunkCache、光照存储、实体 section...)
 * 都会在 ±33,554,432 方块处回绕 → 更远的区块永远无法渲染/生成。
 *
 * 我们改为 X/Z 各 28 位、Y 8 位(世界高度只有 4064 格 = 254 个 section,8 位足够)。
 * 渲染/生成上限随之移到 ±2,147,483,632 方块;BlockPos 的方块节点打包(26 位)保持
 * 原样,让它在 33,554,432 处的溢出产生本模组要观察的可见损坏。
 *
 * 位运算集中在 {@link SectionEncoding},方便单元测试。
 * 当 {@link FarLandsProbeConfig#isExtendSectionEncoding()} 关闭时,所有注入
 * 都回退到原版 22/20/22 打包。
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
