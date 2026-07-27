package net.atlas.atlascore.client.gui.elements;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntegerSlider extends AbstractSliderButton {
    private final int min;
    private final int max;
    private final Consumer<Integer> setCallback;
    private final Supplier<Component> messageSupplier;

    public IntegerSlider(final int x, final int y, final int width, final int height, final double initialValue, int min, int max, Consumer<Integer> setCallback, Supplier<Component> messageSupplier) {
        super(x, y, width, height, Component.empty(), initialValue);
        this.min = min;
        this.max = max;
        this.setCallback = setCallback;
        this.messageSupplier = messageSupplier;
    }

    public void updateMessage() {
        this.setMessage(this.messageSupplier.get());
    }

    protected void applyValue() {
        this.setCallback.accept((int)(this.min + Math.abs(this.max - this.min) * this.value));
    }

    public double getProgress() {
        return this.value;
    }

    public void setProgress(double integer) {
        this.value = integer;
    }

    public void setValue(double integer) {
        super.setValue(integer);
    }
}
