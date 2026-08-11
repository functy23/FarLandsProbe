package me.farlandsprobe.mixin.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * Fullbright: the Darkness effect's fog is removed entirely.
 */
@Environment(EnvType.CLIENT)
@Mixin(DarknessFogEnvironment.class)
public class DarknessFogEnvironmentMixin {
	/**
	 * @author farlandsprobe
	 * @reason Remove darkness fog.
	 */
	@Overwrite
	public void setupFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker) {
	}

	/**
	 * @author farlandsprobe
	 * @reason Remove darkness fog.
	 */
	@Overwrite
	public float getModifiedDarkness(LivingEntity entity, float darkness, float partialTickTime) {
		return darkness;
	}
}
