package me.farlandsprobe.mixin;

import java.util.Set;
import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * PathNavigationRegion 会构建一个尺寸由 (endSection - startSection) 决定的区块
 * 数组。在 int 方块边界(±2^31)处该差值回绕、尺寸变成负数 → 生物(如鹦鹉螺)寻路
 * 时抛 NegativeArraySizeException。一旦生物超过约 2,000,000,000 格,我们直接跳过
 * 寻路(返回 null),避免游戏在那里崩溃。
 * 当 {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} 关闭时本注入失效。
 */
@Mixin(PathNavigation.class)
public abstract class PathNavigationMixin {
	private static final double PATHFIND_LIMIT = 2_000_000_000.0;

	@Shadow @Final protected Mob mob;

	@Inject(method = "createPath(Ljava/util/Set;IZI)Lnet/minecraft/world/level/pathfinder/Path;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$noPathfindAtIntEdge(Set<BlockPos> targets, int maxPathLength, boolean above, int reachRange, CallbackInfoReturnable<Path> cir) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return;
		}
		double x = this.mob.getX();
		double z = this.mob.getZ();
		if (x > PATHFIND_LIMIT || x < -PATHFIND_LIMIT || z > PATHFIND_LIMIT || z < -PATHFIND_LIMIT) {
			cir.setReturnValue(null);
		}
	}
}
