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
 * PathNavigationRegion builds a chunk array whose size is derived from
 * (endSection - startSection). At the int block edge (+-2^31) that difference
 * wraps and the size becomes negative -> NegativeArraySizeException while a mob
 * (e.g. nautilus) pathfinds. Once a mob is beyond ~2,000,000,000 blocks we
 * simply skip pathfinding (return null) so the game does not crash there.
 * Disabled when {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} is off.
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
