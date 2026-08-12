package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.SectionEncoding;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在有符号 int 方块边界附近,世界生成会请求一个坐标发生回绕的邻接区块
 * (区块 134217728 在 int 里变成 -134217728)。StaticCache2D 的范围检查会拒绝它。
 * 我们按 28 位 section 回绕语义(SectionPosMixin 使用的打包方式)把回绕坐标重新
 * 映射回缓存范围内,使边界区块能正常生成,而不是抛 "Requested out of range value"。
 * 回绕周期见 {@link SectionEncoding#WRAP_PERIOD}。
 * 当 {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} 关闭时本注入失效。
 */
@Mixin(StaticCache2D.class)
public abstract class StaticCache2DMixin<T> {
	@Shadow @Final private int minX;
	@Shadow @Final private int minZ;
	@Shadow @Final private int sizeX;
	@Shadow @Final private int sizeZ;
	@Shadow @Final private Object[] cache;

	@Inject(method = "get(II)Ljava/lang/Object;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$wrapCoordinates(int x, int z, CallbackInfoReturnable<T> cir) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return;
		}
		int maxX = this.minX + this.sizeX - 1;
		int maxZ = this.minZ + this.sizeZ - 1;
		if (x >= this.minX && x <= maxX && z >= this.minZ && z <= maxZ) {
			return;
		}
		int fx = fixCoord(this.minX, maxX, x);
		int fz = fixCoord(this.minZ, maxZ, z);
		if (fx != x || fz != z) {
			int deltaX = fx - this.minX;
			int deltaZ = fz - this.minZ;
			cir.setReturnValue((T) this.cache[deltaX * this.sizeZ + deltaZ]);
		}
	}

	private static int fixCoord(int min, int max, int v) {
		long period = SectionEncoding.WRAP_PERIOD;
		long fixed = v - Math.floorDiv((long) v - min, period) * period;
		while (fixed < min) {
			fixed += period;
		}
		while (fixed > max) {
			fixed -= period;
		}
		if (fixed >= min && fixed <= max) {
			return (int) fixed;
		}
		return v;
	}
}
