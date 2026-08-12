package me.farlandsprobe.mixin;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
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
 * ±33,554,432 方块坐标编码边界上的两个问题:
 *
 * 1) 服务器会把每个玩家/载具移动包的目标坐标夹到 ±30,000,000(clampHorizontal),
 *    并 absSnapTo() 到夹取后的位置,因此走过 30M(或从飞行状态落下)会瞬间被
 *    拉回。本重定向中和该夹取(当
 *    {@link FarLandsProbeConfig#isDisableMovementClamps()} 关闭时回退到原版)。
 *
 * 2) 坐标编码一旦回绕,移动包可能带着数千万格的位移到达;Entity.move() 会对一个
 *    巨大的 AABB 跑 BlockCollisions,服务器线程因此卡死("半冻结":地形停止加载、
 *    指令失效、UI 仍能渲染)。我们记录异常位移并直接瞬移到目标,不做碰撞
 *    (仅在 {@link FarLandsProbeConfig#isGuardHugeMoveDelta()} 开启时生效)。
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	private static final Logger LOGGER = LoggerFactory.getLogger("farlandsprobe");

	@Shadow @Final private double lastGoodX;
	@Shadow @Final private double lastGoodZ;

	@Redirect(method = "clampHorizontal", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(DDD)D"))
	private static double farlandsprobe$noClampHorizontal(double value, double min, double max) {
		return FarLandsProbeConfig.isDisableMovementClamps() ? value : Mth.clamp(value, min, max);
	}

	@Redirect(
		method = "handleMovePlayer",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V")
	)
	private void farlandsprobe$guardHugeMoveDelta(ServerPlayer instance, MoverType moverType, Vec3 delta) {
		if (!FarLandsProbeConfig.isGuardHugeMoveDelta()) {
			instance.move(moverType, delta);
			return;
		}

		// 位移长度 > 4096 格 = 坐标回绕产物(正常移动远小于此)。
		double lenSq = delta.lengthSqr();
		if (lenSq > 4096.0 * 4096.0) {
			LOGGER.warn(
				"[farlandsprobe] huge move delta len={} pos=({},{},{}) delta=({},{},{}) lastGood=({},{})",
				Math.sqrt(lenSq), instance.getX(), instance.getY(), instance.getZ(),
				delta.x, delta.y, delta.z, this.lastGoodX, this.lastGoodZ
			);
			// setPos 直接瞬移、跳过碰撞:实体穿墙(可接受——回绕后碰撞体积本身已无意义)。
			instance.setPos(instance.getX() + delta.x, instance.getY() + delta.y, instance.getZ() + delta.z);
		} else {
			instance.move(moverType, delta);
		}
	}
}
