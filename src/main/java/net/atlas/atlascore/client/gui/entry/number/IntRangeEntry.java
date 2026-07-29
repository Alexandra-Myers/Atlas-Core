package net.atlas.atlascore.client.gui.entry.number;

import com.mojang.serialization.DataResult;
import net.atlas.atlascore.client.gui.entry.ConfigEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class IntRangeEntry extends NumberRangeEntry<Integer> {
    public IntRangeEntry(Integer currentValue, Supplier<Integer> defaultValue, Integer min, Integer max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Integer> saveCallback) {
        super(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback);
    }

    public static ConfigEntry<?> accept(boolean slider, Integer currentValue, Supplier<Integer> defaultValue, int min, int max, boolean restartRequired, Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Integer> saveCallback) {
        return slider ? new IntSliderEntry(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback) : new IntRangeEntry(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback);
    }

    @Override
    public DataResult<Integer> parseFromString(String value) throws NumberFormatException {
        return DataResult.success(Integer.parseInt(value));
    }

    @Override
    public Integer lowStarting() {
        return Math.max(0, this.min);
    }

    @Override
    public Integer highStarting() {
        return Math.min(0, this.max);
    }

    @Override
    public void increment() {
        this.value = Math.min(this.value + 1, this.max);
    }

    @Override
    public void decrement() {
        this.value = Math.min(this.value - 1, this.max);
    }

    @Override
    public boolean isAtMin(Integer value) {
        return value <= this.min;
    }

    @Override
    public boolean isAtMax(Integer value) {
        return value >= this.max;
    }
}
