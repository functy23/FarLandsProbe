package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import me.farlandsprobe.support.OverflowSafeMath;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MineshaftPieces$MineShaftPiece#isInInvalidLocation 用 (x0 + x1) / 2 计算走廊
 * 中点。超过方块坐标 2^30 后 int 加法溢出,中点跳到约 -2^30,生物群系查询会请求
 * 2^31 格之外的区块 → "Requested chunk unavailable during world generation"。
 * 改用防溢出形式 x0 + (x1 - x0) / 2。
 * 当 {@link FarLandsProbeConfig#isFixMineshaftOverflow()} 关闭时本注入失效。
 */
@Mixin(targets = "net.minecraft.world.level.levelgen.structure.structures.MineshaftPieces$MineShaftPiece")
public abstract class MineshaftPiecesMixin {
	// 注意:本方法体是原版 isInInvalidLocation 的 1:1 移植,只有走廊中点改成了防溢出写法。
	// 同步上游 Minecraft 更新时,先与原版方法逐行 diff 再合并。
	@Inject(method = "isInInvalidLocation(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/level/levelgen/structure/BoundingBox;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$overflowSafeMidpoint(LevelAccessor level, BoundingBox chunkBB, CallbackInfoReturnable<Boolean> cir) {
		if (!FarLandsProbeConfig.isFixMineshaftOverflow()) {
			return;
		}
		int x0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minX() - 1, chunkBB.minX());
		int y0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minY() - 1, chunkBB.minY());
		int z0 = Math.max(((StructurePiece) (Object) this).getBoundingBox().minZ() - 1, chunkBB.minZ());
		int x1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxX() + 1, chunkBB.maxX());
		int y1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxY() + 1, chunkBB.maxY());
		int z1 = Math.min(((StructurePiece) (Object) this).getBoundingBox().maxZ() + 1, chunkBB.maxZ());
		// 防溢出中点:原版 (x0+x1)/2 在超过 2^30 后溢出;见 OverflowSafeMath。
		BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(
			OverflowSafeMath.midpoint(x0, x1), OverflowSafeMath.midpoint(y0, y1), OverflowSafeMath.midpoint(z0, z1));
		if (level.getBiome(blockPos).is(BiomeTags.MINESHAFT_BLOCKING)) {
			cir.setReturnValue(true);
			return;
		}

		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				if (level.getBlockState(blockPos.set(x, y0, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x, y1, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x, y, z0)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x, y, z1)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		for (int z = z0; z <= z1; z++) {
			for (int y = y0; y <= y1; y++) {
				if (level.getBlockState(blockPos.set(x0, y, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}

				if (level.getBlockState(blockPos.set(x1, y, z)).liquid()) {
					cir.setReturnValue(true);
					return;
				}
			}
		}

		cir.setReturnValue(false);
	}
}
