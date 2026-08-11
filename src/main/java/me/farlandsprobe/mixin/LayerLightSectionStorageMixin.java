package me.farlandsprobe.mixin;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.lighting.DataLayerStorageMap;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * The lighting engine stores block nodes (BlockPos.asLong, 26-bit X/Z) in its
 * queues. Past +-33,554,432 blocks those wrap, so the engine can dequeue a
 * node whose section has no light data -> vanilla throws NPE in
 * getStoredLevel/setStoredLevel. We make both tolerant: missing sections are
 * skipped (light just stays uncomputed there = visible corruption, which is
 * exactly what this mod exists to observe).
 */
@Mixin(LayerLightSectionStorage.class)
public abstract class LayerLightSectionStorageMixin<M extends DataLayerStorageMap<M>> {
	@Shadow @Final protected M updatingSectionData;
	@Shadow @Final protected LongSet changedSections;

	@Shadow
	protected abstract DataLayer getDataLayer(long sectionNode, boolean updating);

	/**
	 * @author farlandsprobe
	 * @reason Tolerate missing light sections caused by block-node wrapping.
	 */
	@Overwrite
	protected int getStoredLevel(long blockNode) {
		long sectionNode = SectionPos.blockToSection(blockNode);
		DataLayer layer = this.getDataLayer(sectionNode, true);
		return layer == null
			? 0
			: layer.get(
				SectionPos.sectionRelative(BlockPos.getX(blockNode)),
				SectionPos.sectionRelative(BlockPos.getY(blockNode)),
				SectionPos.sectionRelative(BlockPos.getZ(blockNode))
			);
	}

	/**
	 * @author farlandsprobe
	 * @reason Tolerate missing light sections caused by block-node wrapping.
	 */
	@Overwrite
	protected void setStoredLevel(long blockNode, int level) {
		long sectionNode = SectionPos.blockToSection(blockNode);
		DataLayer layer = this.getDataLayer(sectionNode, true);
		if (layer == null) {
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
	}
}
