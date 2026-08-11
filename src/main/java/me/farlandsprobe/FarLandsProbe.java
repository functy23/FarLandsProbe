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
        // Register the Cloth Config screen; saves defaults to config/farlandsprobe.json
        // so the file exists for the user to edit even before opening the UI.
        FarLandsProbeConfig.register();
        FarLandsProbeConfig.save();
        LOGGER.info("[FarLandsProbe] loaded: fullbright={} border={} clamps={} bounds={} sectionEncoding={}",
            FarLandsProbeConfig.isFullBright(),
            FarLandsProbeConfig.isRemoveWorldBorder(),
            FarLandsProbeConfig.isDisableMovementClamps(),
            FarLandsProbeConfig.isRelaxSpawnAndTeleportBounds(),
            FarLandsProbeConfig.isExtendSectionEncoding()
        );
    }
}
