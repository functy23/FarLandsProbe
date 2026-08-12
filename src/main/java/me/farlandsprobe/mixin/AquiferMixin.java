package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在接近 ±2^31 方块上限处,ChunkPos.getMinBlockX()/getMaxBlockX() 的 int 已经
 * 溢出,原版的网格尺寸运算(全 int)接着把乘积 `gridSizeX * gridSizeY *
 * gridSizeZ` 溢出成巨大的正数,于是 `new long[totalGridSize]` 耗尽堆内存
 * (世界生成期间的 OutOfMemoryError)。
 *
 * 用 long 运算防护:只要任一方块坐标距 int 边界 2,000,000,000 以内(即原版的
 * ±5 偏移会溢出),或(荒谬的)网格过大,就禁用含水层而不是分配内存。
 * 当 {@link FarLandsProbeConfig#isFixAquiferOverflow()} 关闭时本注入失效。
 */
@Mixin(Aquifer.class)
public interface AquiferMixin {
	@Inject(
		method = "create(Lnet/minecraft/world/level/levelgen/NoiseChunk;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/levelgen/NoiseRouter;Lnet/minecraft/world/level/levelgen/PositionalRandomFactory;IILnet/minecraft/world/level/levelgen/Aquifer$FluidPicker;)Lnet/minecraft/world/level/levelgen/Aquifer;",
		at = @At("HEAD"),
		cancellable = true
	)
	private static void farlandsprobe$guardAbsurdAquiferGrid(
		NoiseChunk noiseChunk,
		ChunkPos pos,
		NoiseRouter router,
		PositionalRandomFactory positionalRandomFactory,
		int minBlockY,
		int yBlockSize,
		Aquifer.FluidPicker fluidRule,
		CallbackInfoReturnable<Aquifer> cir
	) {
		if (!FarLandsProbeConfig.isFixAquiferOverflow()) {
			return;
		}

		final long INT_EDGE_SAFE_LIMIT = 2_000_000_000L;
		final long MAX_AQUIFER_GRID = 4_000_000L;
		final long MAX_GRID_SIDE = 4096L;

		long minBlockX = pos.getMinBlockX();
		long maxBlockX = pos.getMaxBlockX();
		long minBlockZ = pos.getMinBlockZ();
		long maxBlockZ = pos.getMaxBlockZ();

		// 块坐标已接近/越过 int 边界：原版的 +/-5 与 int 连乘必然溢出 → 直接禁用。
		if (minBlockX < -INT_EDGE_SAFE_LIMIT || maxBlockX > INT_EDGE_SAFE_LIMIT
			|| minBlockZ < -INT_EDGE_SAFE_LIMIT || maxBlockZ > INT_EDGE_SAFE_LIMIT) {
			LoggerFactory.getLogger("farlandsprobe").warn(
				"[farlandsprobe] aquifer near int edge at pos={} minX={} maxX={} minZ={} maxZ={}; disabling aquifer",
				pos, minBlockX, maxBlockX, minBlockZ, maxBlockZ
			);
			cir.setReturnValue(Aquifer.createDisabled(fluidRule));
			return;
		}

		long gridSizeX = ((maxBlockX - 5) >> 4) + 1 - ((minBlockX - 5) >> 4) + 1;
		long gridSizeY = Math.floorDiv((long) minBlockY + yBlockSize + 1, 12) + 1 - (Math.floorDiv((long) minBlockY + 1, 12) - 1) + 1;
		long gridSizeZ = ((maxBlockZ - 5) >> 4) + 1 - ((minBlockZ - 5) >> 4) + 1;
		long total = gridSizeX * gridSizeY * gridSizeZ;
		if (Math.abs(total) > MAX_AQUIFER_GRID || Math.abs(gridSizeX) > MAX_GRID_SIDE
			|| Math.abs(gridSizeY) > MAX_GRID_SIDE || Math.abs(gridSizeZ) > MAX_GRID_SIDE) {
			LoggerFactory.getLogger("farlandsprobe").warn(
				"[farlandsprobe] absurd aquifer grid {}x{}x{}={} at pos={} minY={} height={}; disabling aquifer",
				gridSizeX, gridSizeY, gridSizeZ, total, pos, minBlockY, yBlockSize
			);
			cir.setReturnValue(Aquifer.createDisabled(fluidRule));
		}
	}
}
