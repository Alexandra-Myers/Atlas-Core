package net.atlas.atlaslib.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.atlas.atlaslib.AtlasLib;
import net.atlas.atlaslib.config.AtlasConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;

public final class ScreenBuilder {
    private ScreenBuilder() {
    }

	public static Screen buildAtlasConfig(Screen prevScreen, AtlasConfig config) {
		Screen special = config.createScreen(prevScreen);
		if (special != null)
			return special;
		ConfigBuilder builder = ConfigBuilder.create()
			.setTitle(Component.translatable("text.config." + config.name.getPath() + ".title"))
			.transparentBackground()
			.setSavingRunnable(() -> {
				try {
					config.saveConfig();
				} catch (IOException e) {
					AtlasLib.LOGGER.error("Failed to save " + config.name + " config file!");
					e.printStackTrace();
				}
			});
		if (prevScreen != null) builder.setParentScreen(prevScreen);

		for (AtlasConfig.Category category : config.categories) {
			ConfigCategory configCategory = builder.getOrCreateCategory(Component.translatable(category.translationKey()));

			for (AbstractConfigListEntry<?> entry : category.membersAsCloth()) {
				configCategory.addEntry(entry);
			}
		}

		return builder.build();
	}
}
