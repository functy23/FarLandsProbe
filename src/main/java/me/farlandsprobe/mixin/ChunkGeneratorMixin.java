package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
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
 * Structure pieces (mineshaft, ruined portal, ...) compute bounding boxes with
 * int sums like (x0 + x1) / 2 or minX + width. Past roughly +-2^30 blocks those
 * overflow, feeding garbage coordinates into NoiseChunk/Aquifer and causing
 * OutOfMemoryError during worldgen (huge aquifer grid allocation). Structure
 * generation beyond a safe margin (63,000,000 chunks = ~1,008,000,000 blocks)
 * is skipped entirely; normal terrain generation is unaffected.
 * Disabled when {@link FarLandsProbeConfig#isDisableStructuresFarOut()} is off.
 */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
	private static final long MAX_STRUCTURE_CHUNK = 63_000_000L;

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
}
