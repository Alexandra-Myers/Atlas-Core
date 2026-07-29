package net.atlas.atlascore.client.gui.entry.number;

import com.mojang.serialization.DataResult;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LongRangeEntry extends NumberRangeEntry<Long> {

    public LongRangeEntry(Long currentValue, Supplier<Long> defaultValue, Long min, Long max, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Long> saveCallback) {
        super(currentValue, defaultValue, min, max, restartRequired, name, tooltip, saveCallback);
    }

    @Override
    public DataResult<Long> parseFromString(String value) throws NumberFormatException {
        return DataResult.success(Long.parseLong(value));
    }

    @Override
    public Long lowStarting() {
        return Math.max(0, this.min);
    }

    @Override
    public Long highStarting() {
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
    public boolean isAtMin(Long value) {
        return value <= this.min;
    }

    @Override
    public boolean isAtMax(Long value) {
        return value >= this.max;
    }
}
