package me.farlandsprobe.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Fullbright: raw brightness queries (used by crop growth, mob spawning checks,
 * pathfinding, debug overlays) always report 15.
 */
@Mixin(LevelLightEngine.class)
public abstract class LevelLightEngineMixin {
	/**
	 * @author farlandsprobe
	 * @reason Force every block to report maximum brightness (global no-darkness).
	 */
	@Overwrite
	public int getRawBrightness(BlockPos pos, int skyDampen) {
		return 15;
	}
}
