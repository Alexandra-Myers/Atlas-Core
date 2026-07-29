package net.atlas.atlascore.client.gui.entry.number;

import net.atlas.atlascore.client.gui.elements.IntegerSlider;
import net.atlas.atlascore.client.gui.entry.ConfigEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntSliderEntry extends ConfigEntry<Integer> {
    public final Integer min;
    public final Integer max;
    public final IntegerSlider slider;
    public IntSliderEntry(Integer currentValue, Supplier<Integer> defaultValue, Integer min, Integer max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Integer> saveCallback) {
        super(currentValue == null ? min : currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.min = min;
        this.max = max;
        double initialPosition = (double) (getValue() - min) / Mth.abs(max - min);
        this.slider = new IntegerSlider(0, 0, 48, 20, initialPosition, min, max, this::setValue, () -> Component.literal(String.format("Value: %d", this.getValue())));
        this.addChild(this.slider);
    }


    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.slider.active = isEditable();
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        this.slider.setPosition(valueX, getPaddedY());
        this.slider.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getValueWidth() {
        return 48;
    }
}
