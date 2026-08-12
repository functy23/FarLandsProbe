package me.farlandsprobe.mixin.client;

import java.lang.reflect.Field;
import me.farlandsprobe.config.FarLandsProbeConfig;
import org.slf4j.Logger;
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
 * 渲染器的 Octree 把可见区域包进一个 2 的幂大小的 BoundingBox。
 * 在接近 ±2^31 方块上限处,`minX + 512 - 1` 的 int 溢出会使包围盒反转,整个场景
 * 停止渲染。我们用 long 运算重建包围盒,把它平移回 int 范围内,同时保持 512 格
 * 的 2 的幂跨度(这样 octree 的细分不变量仍然成立)。
 * 最后的 `root` 字段通过缓存反射 Field 写入,因为 JVM 禁止从注入方法里写 final 字段。
 * 当 {@link FarLandsProbeConfig#isFixOctreeOverflow()} 关闭时本注入失效。
 */
@Environment(EnvType.CLIENT)
@Mixin(Octree.class)
public abstract class OctreeMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("farlandsprobe");
	private static final Field ROOT_FIELD;

	static {
		try {
			ROOT_FIELD = Octree.class.getDeclaredField("root");
			ROOT_FIELD.setAccessible(true);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(
				"farlandsprobe: cannot access Octree.root - field renamed in this Minecraft version? "
					+ "Check OctreeMixin and update the field name",
				e
			);
		}
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void farlandsprobe$clampOctreeBounds(SectionPos cameraSection, int renderDistance, int sectionsPerChunk, int minBlockY, CallbackInfo ci) {
		if (!FarLandsProbeConfig.isFixOctreeOverflow()) {
			return;
		}
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

		// section 坐标 1.3e8 x 16 格 ≈ 2.08e9,已接近 int 边界:此时才打印诊断日志。
		if (Math.abs(cameraSection.x()) > 130000000 || Math.abs(cameraSection.z()) > 130000000) {
			LOGGER.info(
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
