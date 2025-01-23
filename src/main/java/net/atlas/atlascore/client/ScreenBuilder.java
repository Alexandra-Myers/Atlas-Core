package net.atlas.atlascore.client;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.List;

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
					AtlasCore.LOGGER.error("Failed to save " + config.name + " config file!");
					e.printStackTrace();
				}
			});
		if (prevScreen != null) builder.setParentScreen(prevScreen);

		if (!config.categories.isEmpty()) {
			for (AtlasConfig.Category category : config.categories) {
				ConfigCategory configCategory = builder.getOrCreateCategory(Component.translatable(category.translationKey()));
	
				for (AbstractConfigListEntry<?> entry : category.membersAsCloth()) {
					configCategory.addEntry(entry);
				}
			}
		}
		List<AtlasConfig.ConfigHolder<?>> uncategorised = config.getUncategorisedHolders();
		if (!uncategorised.isEmpty()) {
			ConfigCategory configCategory = builder.getOrCreateCategory(Component.translatable("text.config.misc_category"));
			uncategorised.stream().map(holder -> {
				AbstractConfigListEntry<?> entry = holder.transformIntoConfigEntry();
				entry.setEditable(!holder.serverManaged);
				return entry;
			}).forEach(configCategory::addEntry);
		}

		return builder.build();
	}
}
