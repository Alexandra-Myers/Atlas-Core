package net.atlas.atlascore.client.gui.entry.number;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FloatRangeEntry extends NumberRangeEntry<Float> {
    public FloatRangeEntry(Float currentValue, Supplier<Float> defaultValue, Float min, Float max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Float> saveCallback) {
        super(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback);
    }

    @Override
    public DataResult<Float> parseFromString(String value) throws NumberFormatException {
        return DataResult.success(Float.parseFloat(value));
    }

    @Override
    public Float lowStarting() {
        return Math.max(0, this.min);
    }

    @Override
    public Float highStarting() {
        return Math.min(0, this.max);
    }

    @Override
    public void increment() {
        this.value = Math.min(this.value + 1, this.max);
    }

    @Override
    public void decrement() {
        this.value = Math.max(this.value - 1, this.min);
    }

    @Override
    public boolean isAtMin(Float value) {
        return value <= this.min;
    }

    @Override
    public boolean isAtMax(Float value) {
        return value >= this.max;
    }
}
