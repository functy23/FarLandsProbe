package me.farlandsprobe.mixin.client;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.DarknessFogEnvironment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fullbright: the Darkness effect's fog is removed entirely.
 * Disabled when {@link FarLandsProbeConfig#isFullBright()} is off (vanilla darkness fog returns).
 */
@Environment(EnvType.CLIENT)
@Mixin(DarknessFogEnvironment.class)
public class DarknessFogEnvironmentMixin {
	@Inject(method = "setupFog(Lnet/minecraft/client/renderer/fog/FogData;Lnet/minecraft/client/Camera;Lnet/minecraft/client/multiplayer/ClientLevel;FLnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$removeDarknessFog(FogData fog, Camera camera, ClientLevel level, float renderDistance, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (FarLandsProbeConfig.isFullBright()) {
			ci.cancel();
		}
	}

	@Inject(method = "getModifiedDarkness(Lnet/minecraft/world/entity/LivingEntity;FF)F", at = @At("HEAD"), cancellable = true)
	private void farlandsprobe$keepDarkness(LivingEntity entity, float darkness, float partialTickTime, CallbackInfoReturnable<Float> cir) {
		if (FarLandsProbeConfig.isFullBright()) {
			cir.setReturnValue(darkness);
		}
	}
}
