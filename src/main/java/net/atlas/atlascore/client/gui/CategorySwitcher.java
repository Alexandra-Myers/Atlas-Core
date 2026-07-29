package net.atlas.atlascore.client.gui;

import com.google.common.collect.ImmutableList;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.util.ClientUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

public class CategorySwitcher extends AbstractContainerWidget {
    private static final int MAX_VISIBLE = 7;
    public static final WidgetSprites LEFT_SPRITE = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_shift_left"));
    public static final WidgetSprites RIGHT_SPRITE = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_shift_right"));
    public static final Component LEFT = Component.translatableWithFallback("text.config.shift_left", "Shift Left");
    public static final Component RIGHT = Component.translatableWithFallback("text.config.shift_right", "Shift Right");
    private final int visibleCount;
    private final Button left;
    private final Button right;
    private final List<Button> categories;
    private final List<? extends GuiEventListener> listeners;
    private int currentIndex = 0;

    public CategorySwitcher(ConfigScreen screen, List<ConfigCategory> categories) {
        super(0, 38, screen.width, 20, Component.empty()/*? <26.2 {*/, AbstractScrollArea.defaultSettings(0)/*?}*/);
        int buttonsWidth = screen.width - 20;
        this.visibleCount = Math.max(Math.min(categories.size(), MAX_VISIBLE), 1);
        this.left = SpriteIconButton.builder(LEFT,
                        button -> this.setCurrentIndex(this.currentIndex - 1), true)
                .size(10, 20)
                .sprite(LEFT_SPRITE, 10, 20).build();
        this.left.setPosition(0, 38);
        this.right = SpriteIconButton.builder(RIGHT,
                        button -> this.setCurrentIndex(this.currentIndex + 1), true)
                .size(10, 20)
                .sprite(RIGHT_SPRITE, 10, 20).build();
        this.right.setPosition(this.width - 10, 38);
        this.categories = categories.stream().map(configCategory -> {
            Button ret = Button.builder(configCategory.name, button -> {
                screen.setSelectedCategory(configCategory);
                this.getCategories().forEach(button1 -> button1.active = true);
                button.active = false;
            }).size(buttonsWidth / this.visibleCount, 20).build();
            ret.active = configCategory != screen.getSelectedCategory();
            return ret;
        }).toList();
        ImmutableList.Builder<GuiEventListener> builder = ImmutableList.builder();
        builder.addAll(this.categories);
        builder.add(this.left);
        builder.add(this.right);
        this.listeners = builder.build();
    }

    private void setCurrentIndex(int index) {
        this.currentIndex = Mth.clamp(index, 0, this.categories.size());
        for (int i = 0; i < this.categories.size(); i++) {
            if (i < this.currentIndex || !(i < this.currentIndex + this.visibleCount)) {
                this.categories.get(i).active = false;
            }
        }
    }

    public List<Button> getCategories() {
        return this.categories;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.left.active = this.currentIndex > 0;
        this.right.active = (this.currentIndex + this.visibleCount) < this.categories.size();
        int x = getX();
        int y = getY();
        this.left.extractRenderState(graphics, mouseX, mouseY, a);
        x += this.left.getWidth();
        for (int i = this.currentIndex; i < this.categories.size() && i < this.currentIndex + this.visibleCount; i++) {
            Button button = this.categories.get(i);
            button.setPosition(x, y);
            button.extractRenderState(graphics, mouseX, mouseY, a);
            x += button.getWidth();
        }
        this.right.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.left.updateNarration(output);
        this.right.updateNarration(output);
        this.categories.forEach(button -> button.updateNarration(output));
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return this.listeners;
    }

    @Override
    protected int contentHeight() {
        return 0;
    }
}
