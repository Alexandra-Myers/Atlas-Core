package net.atlas.atlascore.client.gui;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.client.gui.entry.ConfigEntry;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static net.atlas.atlascore.client.gui.entry.EnumEntry.snakeCaseToName;

public class ConfigScreen extends Screen {
    private static final Component SAVE_AND_EXIT = Component.translatableWithFallback("text.config.save_and_exit", "Save & Exit");
    private static final Component EXIT_WITHOUT_SAVING = Component.translatableWithFallback("text.config.exit_without_saving", "Exit without Saving");
    @SuppressWarnings("FieldCanBeLocal")
    private /*? >=26.2 {*//*final*//*?}*/ HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 64);
    private final Screen lastScreen;
    private final AtlasConfig config;
    private final List<ConfigCategory> categories = new ArrayList<>();
    private ConfigCategory selectedCategory;
    private Button saveButton;
    public ConfigScreen(Screen lastScreen, Component title, AtlasConfig config) {
        super(title);
        this.lastScreen = lastScreen;
        this.config = config;
    }

    @Override
    protected void init() {
        super.init();
        //? >=26.2
        //this.layout.removeChildren();
        //? <26.2
        this.layout = new HeaderAndFooterLayout(this, 33, 64);
        this.layout.addTitleHeader(this.title, this.font);
        this.categories.clear();
        LinearLayout footer = this.layout.addToFooter(LinearLayout.vertical().spacing(4));
        footer.defaultCellSetting().alignVerticallyMiddle();
        LinearLayout bottomFooterButtons = footer.addChild(LinearLayout.horizontal().spacing(8));
        this.saveButton = Button.builder(SAVE_AND_EXIT, (button) -> {
            boolean isRestartRequired = false;
            for (ConfigCategory category : categories) {
                isRestartRequired |= category.save();
            }
            try {
                this.config.saveConfig();
            } catch (IOException e) {
                AtlasCore.LOGGER.error("Failed to save " + this.config.name + " config file!", e);
            }
            if (!isRestartRequired) this.onClose();
            else ClientUtils.setScreen(this.minecraft, new RestartRequiredScreen(this.lastScreen));
        }).build();
        this.saveButton.active = false;
        bottomFooterButtons.addChild(this.saveButton);
        bottomFooterButtons.addChild(Button.builder(EXIT_WITHOUT_SAVING, (button) -> this.onClose()).build());
        this.layout.arrangeElements();
        this.config.categories.forEach(category -> {
            ConfigCategory configCategory = ConfigCategory.create(Component.translatableWithFallback(category.translationKey(), snakeCaseToName(category.name())), this);
            category.membersAsConfigEntries().forEach(configCategory::addEntry);
            this.categories.add(configCategory);
        });
        List<AtlasConfig.ConfigHolder<?>> uncategorised = this.config.getUncategorisedHolders();
        if (!uncategorised.isEmpty()) {
            ConfigCategory configCategory = ConfigCategory.create(Component.translatableWithFallback("text.config.misc_category", "Default Category"), this);
            uncategorised.stream().map(holder -> {
                ConfigEntry<?> entry = holder.transformIntoRealConfigEntry();
                entry.setEditable(!holder.serverManaged);
                return entry;
            }).forEach(configCategory::addEntry);
            this.categories.add(configCategory);
        }
        this.selectedCategory = this.categories.getFirst();
        this.layout.addToContents(new CategorySwitcher(this, this.categories));
        this.categories.forEach(this.layout::addToContents);
        this.layout.visitWidgets(this::addRenderableWidget);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.saveButton.active = this.categories.stream().noneMatch(ConfigCategory::hasErrors)
                && this.categories.stream().anyMatch(ConfigCategory::isChanged);
        this.categories.forEach(category -> category.visible = Objects.equals(category, this.selectedCategory));
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    public void onClose() {
        ClientUtils.setScreen(this.minecraft, this.lastScreen);
    }

    public Minecraft getMinecraft() {
        return this.minecraft;
    }

    public void setSelectedCategory(ConfigCategory selectedCategory) {
        this.selectedCategory = selectedCategory;
    }

    public ConfigCategory getSelectedCategory() {
        return this.selectedCategory;
    }
}
