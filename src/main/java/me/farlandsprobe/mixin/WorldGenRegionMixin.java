package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
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
 * At the signed int block limit (+-2^31), the chunk right at the edge needs a
 * neighbour chunk whose coordinate wraps (e.g. chunk 134217728 becomes
 * -134217728 in int). Two things break:
 *
 * 1) The chessboard distance computes ~2^28 instead of 1, so the request is
 *    treated as out of dependency range. We compute it with 28-bit section
 *    wrap-around semantics instead.
 *
 * 2) Even with a small distance, the wrapped coordinate may fall outside the
 *    StaticCache2D range check (fixed in StaticCache2DMixin) or the dependency
 *    status may still be null. As a fallback we re-map the wrapped coordinate
 *    back into the cache range and return that chunk directly.
 *
 * Disabled when {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} is off.
 */
@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin {
	private static final long WRAP_PERIOD = 1L << 28;

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
		// 尝试把回绕坐标映射回缓存范围内。
		long[] offsets = {0L, WRAP_PERIOD, -WRAP_PERIOD, 2 * WRAP_PERIOD, -2 * WRAP_PERIOD};
		for (long dx : offsets) {
			for (long dz : offsets) {
				int tx = (int) (chunkX + dx);
				int tz = (int) (chunkZ + dz);
				if (this.cache.contains(tx, tz)) {
					GenerationChunkHolder holder = this.cache.get(tx, tz);
					if (holder != null) {
						ChunkAccess chunk = holder.getChunkIfPresentUnchecked(targetStatus);
						if (chunk != null) {
							cir.setReturnValue(chunk);
							return;
						}
					}
				}
			}
		}
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
		long wrapped = diff % WRAP_PERIOD;
		if (wrapped > WRAP_PERIOD / 2) {
			wrapped = WRAP_PERIOD - wrapped;
		}
		return (int) wrapped;
	}
}
