package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 两个远边界防护:
 *
 * 1) 结构件(废弃矿井、废弃传送门...)用 int 求和计算包围盒,在约 ±2^30 之后
 *    会溢出,把垃圾数据喂给 NoiseChunk/Aquifer 并导致 OOM。超过安全裕量
 *    (63,000,000 个区块)之后的结构生成被整体跳过。
 *
 * 2) 特征生成(矿石、树...)会探测区块周边位置;在 int 方块边界处这些探测
 *    溢出并请求不可用的区块("Requested chunk unavailable during world
 *    generation")。因此 ±2^31 前的最后几个区块会跳过特征装饰。
 *
 * 两者在对应配置开关关闭时失效。
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	private static final long MAX_STRUCTURE_CHUNK = 63_000_000L;
	private static final long MAX_FEATURE_CHUNK = 134_217_725L; // 方块 2,147,483,600

	@Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$skipStructuresNearCoordinateLimit(
		RegistryAccess registryAccess,
		ChunkGeneratorStructureState state,
		StructureManager structureManager,
		ChunkAccess centerChunk,
		StructureTemplateManager structureTemplateManager,
		ResourceKey<Level> level,
		CallbackInfo ci
	) {
		if (!FarLandsProbeConfig.isDisableStructuresFarOut()) {
			return;
		}
		ChunkPos pos = centerChunk.getPos();
		long cx = pos.x();
		long cz = pos.z();
		if (cx > MAX_STRUCTURE_CHUNK || cx < -MAX_STRUCTURE_CHUNK || cz > MAX_STRUCTURE_CHUNK || cz < -MAX_STRUCTURE_CHUNK) {
			ci.cancel();
		}
	}

	@Inject(method = "applyBiomeDecoration(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/world/level/StructureManager;)V", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$skipFeaturesNearIntEdge(WorldGenLevel level, ChunkAccess chunk, StructureManager structureManager, CallbackInfo ci) {
		if (!FarLandsProbeConfig.isFixChunkBoundaryGeneration()) {
			return;
		}
		ChunkPos pos = chunk.getPos();
		long cx = pos.x();
		long cz = pos.z();
		if (cx > MAX_FEATURE_CHUNK || cx < -MAX_FEATURE_CHUNK || cz > MAX_FEATURE_CHUNK || cz < -MAX_FEATURE_CHUNK) {
			ci.cancel();
		}
	}
}
