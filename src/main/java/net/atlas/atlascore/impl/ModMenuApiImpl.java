package net.atlas.atlascore.impl;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.atlas.atlascore.client.ScreenBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasConfigScreen;
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
