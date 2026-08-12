package me.farlandsprobe.mixin;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.longs.LongSortedSets;
import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 实体 section 查询会用 subSet(asLong(x, 0, 0), asLong(x, -1, -1) + 1) 构建打包
 * long 的范围。在最大正 section 坐标(约 33,554,416 格)处,+1 溢出成
 * Long.MIN_VALUE,导致 start > end 并使游戏崩溃(如准星射线检测)。
 * 给范围加防护:退化成空集合而不是崩溃。
 * 当 {@link FarLandsProbeConfig#isFixEntitySectionOverflow()} 关闭时本注入失效。
 */
@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin<T extends EntityAccess> {
	@Redirect(
		method = {"forEachAccessibleNonEmptySection", "getChunkSections"},
		at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSortedSet;subSet(JJ)Lit/unimi/dsi/fastutil/longs/LongSortedSet;")
	)
	private LongSortedSet farlandsprobe$safeSectionSubSet(LongSortedSet instance, long from, long to) {
		if (!FarLandsProbeConfig.isFixEntitySectionOverflow()) {
			return instance.subSet(from, to);
		}
		return from <= to ? instance.subSet(from, to) : LongSortedSets.EMPTY_SET;
	}
}
