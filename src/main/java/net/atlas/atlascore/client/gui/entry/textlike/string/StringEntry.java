package net.atlas.atlascore.client.gui.entry.textlike.string;

import com.mojang.serialization.DataResult;
import net.atlas.atlascore.client.gui.entry.textlike.TextLikeEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class StringEntry extends TextLikeEntry<String> {
    private final int minLen;
    private final int maxLen;
    @Nullable
    private final Pattern pattern;

    public StringEntry(String currentValue, Supplier<String> defaultValue, int minLen, int maxLen, @Nullable Pattern pattern, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<String> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.minLen = minLen;
        this.maxLen = maxLen;
        this.pattern = pattern;
    }

    @Override
    public DataResult<String> parseFromString(String value) {
        if (value.length() < this.minLen)
            return DataResult.error(() -> "Value too short! Expected: " + this.minLen + " Actual: " + value.length());
        if (value.length() > this.maxLen)
            return DataResult.error(() -> "Value too long! Expected: " + this.maxLen + " Actual: " + value.length());
        if (this.pattern != null && !this.pattern.matcher(value).matches())
            return DataResult.error(() -> "String does not match pattern! Expected string to match: " + this.pattern + " Got: " + value.length());
        return DataResult.success(value);
    }
}
