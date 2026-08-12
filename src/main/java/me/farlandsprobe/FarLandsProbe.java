package me.farlandsprobe;

import me.farlandsprobe.config.FarLandsProbeConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FarLandsProbe implements ModInitializer {
	public static final String MOD_ID = "farlandsprobe";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// 注册 Cloth Config 界面;先保存默认值到 config/farlandsprobe.json,
		// 这样即使没打开过界面,文件也已存在供用户编辑。
		FarLandsProbeConfig.register();
		FarLandsProbeConfig.save();
		if (FarLandsProbeConfig.isC2meCompatMode()) {
			LOGGER.warn("[FarLandsProbe] C²M Engine detected: 28/8/28 section encoding auto-disabled "
				+ "(c2me is incompatible with the extended packing); other features remain active.");
		}
		LOGGER.info("[FarLandsProbe] loaded: fullbright={} border={} clamps={} bounds={} sectionEncoding={}",
			FarLandsProbeConfig.isFullBright(),
			FarLandsProbeConfig.isRemoveWorldBorder(),
			FarLandsProbeConfig.isDisableMovementClamps(),
			FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds(),
			FarLandsProbeConfig.isExtendSectionEncoding()
		);
	}
}
