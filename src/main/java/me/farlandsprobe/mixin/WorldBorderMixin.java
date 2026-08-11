package me.farlandsprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * World border removal: all "is inside bounds" checks always pass (no damage,
 * no interaction blocking, no entity-spawn gate), the physical push wall and
 * red-vignette are disabled, and clamping is a no-op.
 */
@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
	/**
	 * @author farlandsprobe
	 * @reason Belt-and-suspenders: the border collision box is always empty,
	 * so no code path can ever build a physical wall out of it.
	 */
	@Overwrite
	public VoxelShape getCollisionShape() {
		return Shapes.empty();
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(BlockPos pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(Vec3 pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(ChunkPos pos) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(AABB aabb) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(double x, double z) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove the built-in world border.
	 */
	@Overwrite
	public boolean isWithinBounds(double x, double z, double margin) {
		return true;
	}

	/**
	 * @author farlandsprobe
	 * @reason Disable the border's push/collision wall and close-to-border checks.
	 */
	@Overwrite
	public boolean isInsideCloseToBorder(net.minecraft.world.entity.Entity source, AABB boundingBox) {
		return false;
	}

	/**
	 * @author farlandsprobe
	 * @reason Force the red vignette to never appear.
	 */
	@Overwrite
	public double getDistanceToBorder(double x, double z) {
		return Double.MAX_VALUE;
	}

	/**
	 * @author farlandsprobe
	 * @reason Never clamp positions to the border.
	 */
	@Overwrite
	public BlockPos clampToBounds(BlockPos position) {
		return position;
	}

	/**
	 * @author farlandsprobe
	 * @reason Never clamp positions to the border.
	 */
	@Overwrite
	public BlockPos clampToBounds(double x, double y, double z) {
		return BlockPos.containing(x, y, z);
	}

	/**
	 * @author farlandsprobe
	 * @reason Never clamp positions to the border.
	 */
	@Overwrite
	public Vec3 clampVec3ToBound(Vec3 position) {
		return position;
	}

	/**
	 * @author farlandsprobe
	 * @reason Never clamp positions to the border.
	 */
	@Overwrite
	public Vec3 clampVec3ToBound(double x, double y, double z) {
		return new Vec3(x, y, z);
	}
}
