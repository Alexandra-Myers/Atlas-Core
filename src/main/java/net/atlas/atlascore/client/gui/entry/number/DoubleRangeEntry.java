package net.atlas.atlascore.client.gui.entry.number;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DoubleRangeEntry extends NumberRangeEntry<Double> {
    public DoubleRangeEntry(Double currentValue, Supplier<Double> defaultValue, Double min, Double max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Double> saveCallback) {
        super(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback);
    }

    @Override
    public DataResult<Double> parseFromString(String value) throws NumberFormatException {
        return DataResult.success(Double.parseDouble(value));
    }

    @Override
    public Double lowStarting() {
        return Math.max(0, this.min);
    }

    @Override
    public Double highStarting() {
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
    public boolean isAtMin(Double value) {
        return value <= this.min;
    }

    @Override
    public boolean isAtMax(Double value) {
        return value >= this.max;
    }
}
