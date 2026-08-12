package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 移除世界边界:所有"是否在边界内"的判断恒为真(无伤害、无交互阻挡、无实体生成
 * 门禁),物理推墙与红色暗角被禁用,夹取变为空操作。
 * 当 {@link FarLandsProbeConfig#isRemoveWorldBorder()} 关闭时,所有注入都回退到
 * 原版实现。
 */
@Mixin(WorldBorder.class)
public abstract class WorldBorderMixin {
	@Inject(method = "getCollisionShape()Lnet/minecraft/world/phys/shapes/VoxelShape;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$emptyCollisionShape(CallbackInfoReturnable<VoxelShape> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(Shapes.empty());
		}
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsBlockPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/world/phys/Vec3;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsVec3(Vec3 pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/world/level/ChunkPos;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsChunkPos(ChunkPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isWithinBounds(Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsAabb(AABB aabb, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isWithinBounds(DD)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsDouble(double x, double z, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isWithinBounds(DDD)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$withinBoundsDoubleMargin(double x, double z, double margin, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isInsideCloseToBorder(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$notCloseToBorder(net.minecraft.world.entity.Entity source, AABB boundingBox, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(false);
		}
	}

	@Inject(method = "getDistanceToBorder(DD)D", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$infiniteBorderDistance(double x, double z, CallbackInfoReturnable<Double> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(Double.MAX_VALUE);
		}
	}

	@Inject(method = "clampToBounds(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$noClampBlockPos(BlockPos position, CallbackInfoReturnable<BlockPos> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(position);
		}
	}

	@Inject(method = "clampToBounds(DDD)Lnet/minecraft/core/BlockPos;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$noClampBlockPosXyz(double x, double y, double z, CallbackInfoReturnable<BlockPos> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(BlockPos.containing(x, y, z));
		}
	}

	@Inject(method = "clampVec3ToBound(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$noClampVec3(Vec3 position, CallbackInfoReturnable<Vec3> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(position);
		}
	}

	@Inject(method = "clampVec3ToBound(DDD)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$noClampVec3Xyz(double x, double y, double z, CallbackInfoReturnable<Vec3> cir) {
		if (FarLandsProbeConfig.isRemoveWorldBorder()) {
			cir.setReturnValue(new Vec3(x, y, z));
		}
	}
}
