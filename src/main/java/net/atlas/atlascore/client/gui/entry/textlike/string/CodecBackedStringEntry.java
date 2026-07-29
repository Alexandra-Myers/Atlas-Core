package net.atlas.atlascore.client.gui.entry.textlike.string;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.atlas.atlascore.client.gui.entry.textlike.TextLikeEntry;
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

public class CodecBackedStringEntry<T> extends TextLikeEntry<JsonElement> {
    private final Either<Schema<T>, SchemaCodec<T>> schema;
    public CodecBackedStringEntry(Either<Schema<T>, SchemaCodec<T>> schema, JsonElement currentValue, Supplier<JsonElement> defaultValue, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.schema = schema;
    }

    @Override
    public DataResult<JsonElement> parseFromString(String value) {
        JsonReader reader = CodecUI.GSON.newJsonReader(new StringReader(value));
        JsonElement read;
        try {
            read = JsonParser.parseReader(reader);
        } catch (Exception e) {
            return DataResult.error(() -> "Failed to parse JSON string: " + e.getMessage());
        }
        return DataResult.success(read);
    }

    @Override
    public String valueToString(JsonElement value) {
        return CodecUI.GSON.toJson(value);
    }

    @Override
    public Optional<Component> error() {
        return this.schema.right().map(codec -> {
            DynamicOps<JsonElement> ops = SchemaContext.getRegistries().createSerializationContext(JsonOps.INSTANCE);
            return codec.parse(ops, getValue());
        }).flatMap(DataResult::error)
                .<Component>map(e -> Component.literal("Invalid input: " + e)).or(super::error);
    }
}
