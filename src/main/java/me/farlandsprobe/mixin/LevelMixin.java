package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 放宽边界:允许任意位置的生成、传送、方块交互与高度查询,包括远超原版
 * ±30,000,000 方块上限的地方。
 * 当 {@link FarLandsProbeConfig#isRelaxSpawnAndTeleportBounds()} 关闭时本注入失效。
 */
@Mixin(Level.class)
public abstract class LevelMixin {
	@Inject(method = "isInSpawnableBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private static void farlandsprobe$alwaysSpawnable(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isInWorldBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$alwaysInWorldBounds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "isInValidBounds(Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$alwaysInValidBounds(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds()) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "getHeight(Lnet/minecraft/world/level/levelgen/Heightmap$Types;II)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$heightAnywhere(Heightmap.Types type, int x, int z, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds()) {
			return;
		}
		Level self = (Level) (Object) this;
		if (self.hasChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z))) {
			// +1:高度查询返回顶部非空气方块的上一个方块(与原版语义一致)。
			cir.setReturnValue(self.getChunk(SectionPos.blockToSectionCoord(x), SectionPos.blockToSectionCoord(z)).getHeight(type, x & 15, z & 15) + 1);
		} else {
			cir.setReturnValue(self.getMinY());
		}
	}
}
