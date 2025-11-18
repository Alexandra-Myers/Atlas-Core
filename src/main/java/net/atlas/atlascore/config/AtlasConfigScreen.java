package net.atlas.atlascore.config;

import net.atlas.atlascore.client.ScreenBuilder;
import net.atlas.atlascore.client.gui.ConfigList;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class AtlasConfigScreen extends OptionsSubScreen {
    public ConfigList configs;
	public AtlasConfigScreen(Screen screen, Options options, Component component) {
		super(screen, options, component);
	}

	@Override
	protected void init() {
		super.init();
        this.configs = new ConfigList(this.minecraft, this.width, this.height, 32, this.height - 32, 25);
        this.configs.add(AtlasConfig.configs.values().stream()
                .filter(AtlasConfig::hasScreen)
                .map(config -> Button.builder(config.getFormattedName(), button -> this.minecraft.setScreen(ScreenBuilder.buildAtlasConfig(this, config))))
                .toArray(Button.Builder[]::new));
        this.addWidget(configs);
        this.createFooter();
	}

    protected void createFooter() {
        this.addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.lastScreen))
                        .bounds(this.width / 2 - 100, this.height - 27, 200, 20)
                        .build()
        );
    }

    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {
        this.renderBackground(guiGraphics);
        this.configs.render(guiGraphics, i, j, f);
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 16777215);
        super.render(guiGraphics, i, j, f);
    }
}
