package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.AquiferGridGuard;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.densityfunction.DensitySamplerSet;
import net.minecraft.world.level.levelgen.densityfunction.DensityVolume;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 26.3 版的含水层溢出防护。
 *
 * 26.3 把含水层的创建从 {@code Aquifer.create(NoiseChunk, ChunkPos, NoiseRouter, ...)}
 * 搬到了 {@code Aquifer$Config.create(DensitySamplerSet, PositionalRandomFactory,
 * DensityVolume, FluidPicker)},区块信息改由 {@link DensityVolume} 提供。
 * {@code NoiseBasedAquifer} 构造器里的网格尺寸推导与数组分配和 26.2 完全一致
 * (X/Z 用 {@code v >> 4}、Y 用 {@code floorDiv(v, 12)},再连乘分配
 * {@code FluidStatus[]} 与 {@code long[]}),因此溢出行为也一样,只是入参换了来源。
 *
 * 26.1.2 / 26.2 的同名防护见 {@code AquiferMixin}(注入 {@code Aquifer.create})。
 * 判定逻辑共享 {@link AquiferGridGuard},两个版本只差注入点。
 *
 * 当 {@link FarLandsProbeConfig#isFixAquiferOverflow()} 关闭时本注入失效。
 */
@Mixin(Aquifer.Config.class)
public abstract class AquiferConfigMixin {
	@Inject(
		method = "create(Lnet/minecraft/world/level/levelgen/densityfunction/DensitySamplerSet;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;Lnet/minecraft/world/level/levelgen/densityfunction/DensityVolume;Lnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void farlandsprobe$guardAbsurdAquiferGrid(
		DensitySamplerSet samplers,
		PositionalRandomFactory positionalRandomFactory,
		DensityVolume volume,
		Aquifer.FluidPicker fluidRule,
		CallbackInfoReturnable<Aquifer> cir
	) {
		if (!FarLandsProbeConfig.isFixAquiferOverflow()) {
			return;
		}

		long minBlockX = volume.minBlockX();
		long maxBlockX = volume.maxBlockX();
		long minBlockY = volume.minBlockY();
		long maxBlockY = volume.maxBlockY();
		long minBlockZ = volume.minBlockZ();
		long maxBlockZ = volume.maxBlockZ();

		if (AquiferGridGuard.shouldDisableAquifer(minBlockX, maxBlockX, minBlockY, maxBlockY, minBlockZ, maxBlockZ)) {
			LoggerFactory.getLogger("farlandsprobe").warn(
				"[farlandsprobe] aquifer grid overflow risk at volume X[{}..{}] Y[{}..{}] Z[{}..{}]; disabling aquifer",
				minBlockX, maxBlockX, minBlockY, maxBlockY, minBlockZ, maxBlockZ
			);
			cir.setReturnValue(Aquifer.createDisabled(fluidRule));
		}
	}
}
