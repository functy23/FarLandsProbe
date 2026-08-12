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
 * 光照引擎把方块节点(BlockPos.asLong,X/Z 26 位)存进队列。超过 ±33,554,432
 * 方块后这些节点回绕,引擎可能取出一个所属 section 没有光照数据的节点 → 原版在
 * getStoredLevel/setStoredLevel 里抛 NPE。我们让两者都容错:缺失的 section 直接跳过
 * (该处光照保持未计算 = 可见损坏,正是本模组要观察的现象)。
 * 当 {@link FarLandsProbeConfig#isFixLightSectionCrash()} 关闭时本注入失效。
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
		// updating=true:在更新阶段读取,与原版内部调用点保持一致。
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
		// updating=true:在更新阶段读取,与原版内部调用点保持一致。
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
