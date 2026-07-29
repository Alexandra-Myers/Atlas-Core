package net.atlas.atlascore.client.gui.entry.textlike.string;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.mehvahdjukaar.codecui.CodecUI;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaContext;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.io.StringReader;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class CodecBackedStringEntry<T> extends StringEntry {
    private final Either<Schema<T>, SchemaCodec<T>> schema;
    public CodecBackedStringEntry(Either<Schema<T>, SchemaCodec<T>> schema, String currentValue, Supplier<String> defaultValue, int minLen, int maxLen, @Nullable Pattern pattern, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<String> saveCallback) {
        super(currentValue, defaultValue, minLen, maxLen, pattern, restartRequired, name, tooltip, saveCallback);
        this.schema = schema;
    }

    @Override
    public Optional<Component> error() {
        return this.schema.right().map(codec -> {
            DynamicOps<JsonElement> ops = SchemaContext.getRegistries().createSerializationContext(JsonOps.INSTANCE);
            JsonReader reader = CodecUI.GSON.newJsonReader(new StringReader(this.getValue()));
            JsonElement read;
            try {
                read = JsonParser.parseReader(reader);
            } catch (Exception e) {
                return DataResult.error(e::toString);
            }
            return codec.parse(ops, read);
        }).flatMap(DataResult::error)
                .<Component>map(e -> Component.literal("Invalid input: " + e)).or(super::error);
    }
}
