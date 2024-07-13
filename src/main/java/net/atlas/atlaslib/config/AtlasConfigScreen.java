package net.atlas.atlaslib.config;

import net.atlas.atlaslib.client.ScreenBuilder;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class AtlasConfigScreen extends OptionsSubScreen {
	public AtlasConfigScreen(Screen screen, Options options, Component component) {
		super(screen, options, component);
	}

	@Override
	protected void addOptions() {
		List<AbstractWidget> configButtons = new ArrayList<>();
		AtlasConfig.configs.forEach((resourceLocation, config) -> {
			if (config.hasScreen()) configButtons.add(Button.builder(Component.translatable("text.config." + config.name.getPath() + ".title"), button -> this.minecraft.setScreen(ScreenBuilder.buildAtlasConfig(this, config))).build());
		});
        //noinspection DataFlowIssue
        list.addSmall(configButtons);
	}

	protected void repositionElements() {
		super.repositionElements();
		if (this.list != null) {
			this.list.updateSize(this.width, this.layout);
		}
	}
}
