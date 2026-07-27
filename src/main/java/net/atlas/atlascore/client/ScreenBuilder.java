package net.atlas.atlascore.client;

import net.atlas.atlascore.client.gui.ConfigScreen;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScreenBuilder {
    private ScreenBuilder() {
    }

	public static Screen buildAtlasConfig(Screen prevScreen, AtlasConfig config) {
		Screen special = config.createScreen(prevScreen);
		if (special != null)
			return special;
		return new ConfigScreen(prevScreen, Component.translatable("text.config." + config.name.getPath() + ".title"), config);
	}
}
