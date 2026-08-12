package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.util.StaticCache2D;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * At the signed int block limit, worldgen requests a neighbour chunk whose
 * coordinate wraps (chunk 134217728 becomes -134217728). StaticCache2D's range
 * check then rejects it. We re-map the wrapped coordinate into the cache's
 * range using 28-bit section wrap semantics (the packing used by SectionPosMixin),
 * so edge chunks generate instead of throwing "Requested out of range value".
 * Disabled when {@link FarLandsProbeConfig#isFixChunkBoundaryGeneration()} is off.
 */
@Mixin(StaticCache2D.class)
public abstract class StaticCache2DMixin<T> {
	private static final long WRAP_PERIOD = 1L << 28;

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
		long period = WRAP_PERIOD;
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
