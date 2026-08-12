package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.SectionEncoding;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在有符号 int 方块边界(±2^31)处,边界上的区块需要一个坐标发生回绕的邻接区块
 * (例如区块 134217728 在 int 里变成 -134217728)。这里有两处问题:
 *
 * 1) 棋盘距离会被算成约 2^28 而不是 1,请求因此被判定为超出依赖范围。
 *    我们改为按 28 位 section 回绕语义计算距离。
 *
 * 2) 即使距离很小,回绕后的坐标也可能落在 StaticCache2D 的范围检查之外
 *    (由 StaticCache2DMixin 修复),或依赖状态仍为 null。作为兜底,我们把回绕
 *    坐标重新映射回缓存范围内,直接返回对应区块。
 *
 * 回绕周期为 2^28,定义见 {@link SectionEncoding#WRAP_PERIOD}。
 * 当 {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} 关闭时本注入失效。
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	/** 回绕偏移集合:0、±1、±2 个回绕周期,按距离中心由近到远排列。 */
	private static final long[] WRAP_OFFSETS = {
		0L, SectionEncoding.WRAP_PERIOD, -SectionEncoding.WRAP_PERIOD,
		2 * SectionEncoding.WRAP_PERIOD, -2 * SectionEncoding.WRAP_PERIOD
	};

	@Shadow private StaticCache2D<GenerationChunkHolder> cache;

	@Redirect(
		method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;getChessboardDistance(II)I")
	)
	private int farlandsprobe$wrappedChessboardDistance(ChunkPos center, int chunkX, int chunkZ) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return center.getChessboardDistance(chunkX, chunkZ);
		}
		int dx = wrappedDistance(center.x(), chunkX);
		int dz = wrappedDistance(center.z(), chunkZ);
		return Math.max(dx, dz);
	}

	@Inject(
		method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/ChunkAccess;",
		at = @At("HEAD"),
		cancellable = true
	)
	private void farlandsprobe$wrapChunkRequest(int chunkX, int chunkZ, ChunkStatus targetStatus, boolean loadOrGenerate, CallbackInfoReturnable<ChunkAccess> cir) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return;
		}
		if (this.cache.contains(chunkX, chunkZ)) {
			return; // 正常范围，走原逻辑
		}
		// 尝试把回绕坐标映射回缓存范围内，命中即可直接返回目标区块。
		for (long dx : WRAP_OFFSETS) {
			for (long dz : WRAP_OFFSETS) {
				ChunkAccess chunk = lookupWrappedChunk(chunkX + dx, chunkZ + dz, targetStatus);
				if (chunk != null) {
					cir.setReturnValue(chunk);
					return;
				}
			}
		}
	}

	/** 若回绕后的坐标落在缓存范围内且目标状态已就绪，返回对应区块；否则返回 null。 */
	private ChunkAccess lookupWrappedChunk(long x, long z, ChunkStatus targetStatus) {
		if (!this.cache.contains((int) x, (int) z)) {
			return null;
		}
		GenerationChunkHolder holder = this.cache.get((int) x, (int) z);
		return holder != null ? holder.getChunkIfPresentUnchecked(targetStatus) : null;
	}

	@Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$safeHeightAtEdge(Heightmap.Types type, int x, int z, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return;
		}
		// 请求的区块不在本生成区域的缓存内（例如边界附近坐标溢出/回绕后）。
		// 返回一个不可能的高度，让调用方（如 OreFeature）跳过该位置而不是崩溃。
		if (!this.cache.contains(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
			cir.setReturnValue(Integer.MIN_VALUE);
		}
	}

	private static int wrappedDistance(int a, int b) {
		long diff = Math.abs((long) a - b);
		long wrapped = diff % SectionEncoding.WRAP_PERIOD;
		if (wrapped > SectionEncoding.WRAP_PERIOD / 2) {
			wrapped = SectionEncoding.WRAP_PERIOD - wrapped;
		}
		return (int) wrapped;
	}
}
