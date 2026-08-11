package me.farlandsprobe.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Two problems at the +-33,554,432-block coordinate-encoding limit:
 *
 * 1) The server clamps every player/vehicle move-packet target to +-30,000,000
 *    (clampHorizontal) and absSnapTo()s the clamped position, so walking past
 *    30M (or dropping out of flight) instantly snaps the player back. This
 *    redirect neutralizes that clamp.
 *
 * 2) Once the coordinate encoding wraps, a move packet can arrive with a delta
 *    of tens of millions of blocks; Entity.move() then runs BlockCollisions
 *    over a gigantic AABB and the server thread hangs ("half-freeze": terrain
 *    stops loading, commands stop working, UI keeps rendering). We log the
 *    offending delta and snap directly to the target instead of colliding.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("farlandsprobe");

	@Shadow @Final private double lastGoodX;
	@Shadow @Final private double lastGoodZ;

	@Redirect(method = "clampHorizontal", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
	private static double farlandsprobe$noClampHorizontal(double value, double min, double max) {
		return value;
	}

	@Redirect(
		method = "handleMovePlayer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V")
	)
	private void farlandsprobe$guardHugeMoveDelta(ServerPlayer instance, MoverType moverType, Vec3 delta) {
		double lenSq = delta.lengthSqr();
		if (lenSq > 4096.0 * 4096.0) {
			LOGGER.warn(
				"[farlandsprobe] huge move delta len={} pos=({},{},{}) delta=({},{},{}) lastGood=({},{})",
				Math.sqrt(lenSq), instance.getX(), instance.getY(), instance.getZ(),
				delta.x, delta.y, delta.z, this.lastGoodX, this.lastGoodZ
			);
			instance.setPos(instance.getX() + delta.x, instance.getY() + delta.y, instance.getZ() + delta.z);
		} else {
			instance.move(moverType, delta);
		}
	}
}
