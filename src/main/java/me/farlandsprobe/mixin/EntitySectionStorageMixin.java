package me.farlandsprobe.mixin;

import it.unimi.dsi.fastutil.longs.LongSortedSet;
import it.unimi.dsi.fastutil.longs.LongSortedSets;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntitySectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Entity-section lookups build packed-long ranges with
 * subSet(asLong(x, 0, 0), asLong(x, -1, -1) + 1). At the maximum positive
 * section coordinate (~33,554,416 blocks) the +1 overflows to Long.MIN_VALUE,
 * making start > end and crashing the game (crosshair raycast etc.).
 * Guard the range so it degrades to an empty set instead of crashing.
 */
@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin<T extends EntityAccess> {
	@Redirect(
		method = {"forEachAccessibleNonEmptySection", "getChunkSections"},
		at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/longs/LongSortedSet;subSet(JJ)Lit/unimi/dsi/fastutil/longs/LongSortedSet;")
	)
	private LongSortedSet farlandsprobe$safeSectionSubSet(LongSortedSet instance, long from, long to) {
		return from <= to ? instance.subSet(from, to) : LongSortedSets.EMPTY_SET;
	}
}
