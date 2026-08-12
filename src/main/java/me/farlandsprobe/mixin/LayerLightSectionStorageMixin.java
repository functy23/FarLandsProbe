package me.farlandsprobe.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The lighting engine stores block nodes (BlockPos.asLong, 26-bit X/Z) in its
 * queues. Past +-33,554,432 blocks those wrap, so the engine can dequeue a
 * node whose section has no light data -> vanilla throws NPE in
 * getStoredLevel/setStoredLevel. We make both tolerant: missing sections are
 * skipped (light just stays uncomputed there = visible corruption, which is
 * exactly what this mod exists to observe).
 * Disabled when {@link FarLandsProbeConfig#isFixLightSectionCrash()} is off.
 */
@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageMixin<M extends DataLayerStorageMap<M>> {
	@Shadow @Final protected M updatingSectionData;
	@Shadow @Final protected LongSet changedSections;

	@Shadow
	protected abstract DataLayer getDataLayer(long sectionNode, boolean updating);

	@Inject(method = "getStoredLevel(J)I", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$safeGetStoredLevel(long blockNode, CallbackInfoReturnable<Integer> cir) {
		if (!FarLandsProbeConfig.isFixLightSectionCrash()) {
			return;
		}
		long sectionNode = SectionPos.blockToSection(blockNode);
		// updating=true: read in the update phase, matching vanilla's internal call sites.
		DataLayer layer = this.getDataLayer(sectionNode, true);
		cir.setReturnValue(
			layer == null
				? 0
				: layer.get(
					SectionPos.sectionRelative(BlockPos.getX(blockNode)),
					SectionPos.sectionRelative(BlockPos.getY(blockNode)),
					SectionPos.sectionRelative(BlockPos.getZ(blockNode))
				)
		);
	}

	@Inject(method = "setStoredLevel(JI)V", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$safeSetStoredLevel(long blockNode, int level, CallbackInfo ci) {
		if (!FarLandsProbeConfig.isFixLightSectionCrash()) {
			return;
		}
		long sectionNode = SectionPos.blockToSection(blockNode);
		// updating=true: read in the update phase, matching vanilla's internal call sites.
		DataLayer layer = this.getDataLayer(sectionNode, true);
		if (layer == null) {
			ci.cancel();
			return;
		}

		if (this.changedSections.add(sectionNode)) {
			layer = this.updatingSectionData.copyDataLayer(sectionNode);
		}

		layer.set(
			SectionPos.sectionRelative(BlockPos.getX(blockNode)),
			SectionPos.sectionRelative(BlockPos.getY(blockNode)),
			SectionPos.sectionRelative(BlockPos.getZ(blockNode)),
			level
		);
		ci.cancel();
	}
}
