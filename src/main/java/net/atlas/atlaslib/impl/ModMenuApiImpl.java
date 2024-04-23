package net.atlas.atlaslib.impl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.atlas.atlaslib.client.ScreenBuilder;
import net.atlas.atlaslib.config.AtlasConfig;
import net.atlas.atlaslib.config.AtlasConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class ModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> new AtlasConfigScreen(screen, Minecraft.getInstance().options, Component.translatable("title.atlas_config.name"));
    }

	@Override
	public Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
		Map<String, ConfigScreenFactory<?>> configs = new HashMap<>();
		AtlasConfig.menus.forEach((modID, config) -> configs.put(modID, screen -> ScreenBuilder.buildAtlasConfig(screen, config)));
		return configs;
	}
}
