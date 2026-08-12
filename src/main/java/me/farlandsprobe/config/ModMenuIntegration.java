package me.farlandsprobe.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfigClient;

/**
 * Mod Menu 集成:把 Cloth Config 界面暴露出来,以便从模组列表打开。
 * Mod Menu 本身在运行时是可选的;本入口只在安装了 Mod Menu 时被加载。
 */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return parent -> AutoConfigClient.getConfigScreen(FarLandsProbeConfig.class, parent).get();
	}
}
