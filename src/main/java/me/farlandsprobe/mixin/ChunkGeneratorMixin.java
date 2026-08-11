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
 * Two far-edge guards:
 *
 * 1) Structure pieces (mineshaft, ruined portal, ...) compute bounding boxes with
 *    int sums that overflow past roughly +-2^30, feeding garbage into
 *    NoiseChunk/Aquifer and causing OOM. Structure generation beyond a safe
 *    margin (63,000,000 chunks) is skipped entirely.
 *
 * 2) Feature generation (ore, trees, ...) probes positions around the chunk; at
 *    the int block edge those probes overflow and request unavailable chunks
 *    ("Requested chunk unavailable during world generation"). The last few
 *    chunks before +-2^31 therefore skip feature decoration.
 *
 * Both are disabled when the corresponding config toggle is off.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	private static final long MAX_STRUCTURE_CHUNK = 63_000_000L;
	private static final long MAX_FEATURE_CHUNK = 134_217_725L; // block 2,147,483,600

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
