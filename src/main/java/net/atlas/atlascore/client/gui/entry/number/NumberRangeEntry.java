package net.atlas.atlascore.client.gui.entry.number;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.client.gui.entry.textlike.TextLikeEntry;
import net.atlas.atlascore.util.ClientUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class NumberRangeEntry<N extends Number> extends TextLikeEntry<N> {
    public static final WidgetSprites CONFIG_INCREMENT = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_increment"));
    public static final WidgetSprites CONFIG_DECREMENT = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_decrement"));
    public static final Component INCREMENT = Component.translatableWithFallback("text.config.increment", "Increment");
    public static final Component DECREMENT = Component.translatableWithFallback("text.config.decrement", "Decrement");
    public final N min;
    public final N max;
    public final Button incButton;
    public final Button decButton;
    public NumberRangeEntry(N currentValue, Supplier<N> defaultValue, N min, N max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<N> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.min = min;
        this.max = max;
        this.incButton = SpriteIconButton.builder(INCREMENT, button -> {
                    if (this.getValue() == null) this.setValue(min);
                    increment();
                    this.editBox.setValue(valueToString(this.getValue()));
                }, true)
                .sprite(CONFIG_INCREMENT, 20, 10)
                .size(20, 10).build();
        this.decButton = SpriteIconButton.builder(DECREMENT, button -> {
                    if (this.getValue() == null) this.setValue(max);
                    decrement();
                    this.editBox.setValue(valueToString(this.getValue()));
                }, true)
                .sprite(CONFIG_DECREMENT, 20, 10)
                .size(20, 10).build();
        this.addChild(this.incButton);
        this.addChild(this.decButton);
    }


    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        this.incButton.active = isEditable() && (this.getValue() == null || !isAtMax(this.getValue()));
        this.decButton.active = isEditable() && (this.getValue() == null || !isAtMin(this.getValue()));
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        int buttonX = valueX + this.editBox.getWidth() + 2;
        this.incButton.setPosition(buttonX, getPaddedY());
        this.decButton.setPosition(buttonX, getPaddedY() + getUnpaddedBaseHeight() / 2);
        this.incButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.decButton.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContent(graphics, mouseX, mouseY, hovered, a);
    }

    public abstract void increment();
    public abstract void decrement();
    public abstract boolean isAtMin(N value);
    public abstract boolean isAtMax(N value);

    @Override
    public int getValueWidth() {
        return super.getValueWidth() + 22;
    }
}
