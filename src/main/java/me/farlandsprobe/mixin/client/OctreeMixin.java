package me.farlandsprobe.mixin.client;

import java.lang.reflect.Field;
import org.slf4j.LoggerFactory;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.Octree;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The renderer's Octree wraps the visible area in a power-of-two BoundingBox.
 * Near the +-2^31 block limit, `minX + 512 - 1` overflows int, inverting the
 * box, which makes the whole scene stop rendering. We rebuild the box with
 * long math, shifting it to stay inside the int range while keeping the
 * 512-block power-of-two span (so the octree subdivision invariant holds).
 * The final `root` field is replaced via a cached reflective Field because the
 * JVM forbids writing a final field from an injected method.
 */
@Environment(EnvType.CLIENT)
@Mixin(Octree.class)
public abstract class OctreeMixin {
	private static final Field ROOT_FIELD;

	static {
		try {
			ROOT_FIELD = Octree.class.getDeclaredField("root");
			ROOT_FIELD.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("farlandsprobe: cannot access Octree.root", e);
		}
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void farlandsprobe$clampOctreeBounds(SectionPos cameraSection, int renderDistance, int sectionsPerChunk, int minBlockY, CallbackInfo ci) {
		int visibleAreaDiameterInSections = renderDistance * 2 + 1;
		int boundingBoxSizeInSections = Mth.smallestEncompassingPowerOfTwo(visibleAreaDiameterInSections);
		int distanceToBBEdgeInBlocks = renderDistance * 16;
		BlockPos origin = cameraSection.origin();
		long boxSize = (long) boundingBoxSizeInSections * 16;

		long minX = (long) origin.getX() - distanceToBBEdgeInBlocks;
		long maxX = minX + boxSize - 1;
		if (maxX > 2147483647L) {
			maxX = 2147483647L;
			minX = maxX - boxSize + 1;
		}
		if (minX < -2147483648L) {
			minX = -2147483648L;
			maxX = minX + boxSize - 1;
		}

		long minY = boundingBoxSizeInSections >= sectionsPerChunk ? minBlockY : (long) origin.getY() - distanceToBBEdgeInBlocks;
		long maxY = minY + boxSize - 1;
		if (maxY > 2147483647L) {
			maxY = 2147483647L;
			minY = maxY - boxSize + 1;
		}
		if (minY < -2147483648L) {
			minY = -2147483648L;
			maxY = minY + boxSize - 1;
		}

		long minZ = (long) origin.getZ() - distanceToBBEdgeInBlocks;
		long maxZ = minZ + boxSize - 1;
		if (maxZ > 2147483647L) {
			maxZ = 2147483647L;
			minZ = maxZ - boxSize + 1;
		}
		if (minZ < -2147483648L) {
			minZ = -2147483648L;
			maxZ = minZ + boxSize - 1;
		}

		if (Math.abs(cameraSection.x()) > 130000000 || Math.abs(cameraSection.z()) > 130000000) {
			LoggerFactory.getLogger("farlandsprobe").info(
				"[farlandsprobe] octree box section=({},{},{}) box=({},{},{})-({},{},{}) clampNeeded={}",
				cameraSection.x(), cameraSection.y(), cameraSection.z(),
				minX, minY, minZ, maxX, maxY, maxZ,
				maxX != minX + boxSize - 1 || minX != (long) origin.getX() - distanceToBBEdgeInBlocks
			);
		}

		Octree.Branch newRoot = ((Octree) (Object) this).new Branch(
			new BoundingBox((int) minX, (int) minY, (int) minZ, (int) maxX, (int) maxY, (int) maxZ)
		);
		try {
			ROOT_FIELD.set(this, newRoot);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("farlandsprobe: cannot set Octree.root", e);
		}
	}
}
