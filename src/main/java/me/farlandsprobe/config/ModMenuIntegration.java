package me.farlandsprobe.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

/**
 * Mod Menu integration: exposes the Cloth Config screen so it can be opened from
 * the mod list. Mod Menu itself is optional at runtime; this entrypoint is only
 * loaded when Mod Menu is installed.
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfigClient.getConfigScreen(FarLandsProbeConfig.class, parent).get();
	}
}
