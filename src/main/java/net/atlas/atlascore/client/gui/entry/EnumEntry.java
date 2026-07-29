package net.atlas.atlascore.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.mehvahdjukaar.codecui.Schema;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.atlas.atlascore.util.StringUtils.convertToName;

public class EnumEntry extends ConfigEntry<JsonElement> {
    private final CycleButton<JsonElement> button;

    public EnumEntry(JsonElement currentValue, Supplier<JsonElement> defaultValue, List<JsonElement> values, Function<JsonElement, Component> componentFunction, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        ArrayList<JsonElement> possibleValues = new ArrayList<>(values);
        if (!values.contains(currentValue)) possibleValues.add(currentValue);
        if (!values.contains(defaultValue.get())) possibleValues.add(defaultValue.get());
        this.button = CycleButton.builder(componentFunction, currentValue).withValues(possibleValues)
                .displayState(CycleButton.DisplayState.VALUE).create(0, 0, getValueWidth(), getUnpaddedBaseHeight(), name == null ? Component.empty() : name, (button, value) -> {
            setValue(value);
        });
        this.addChild(this.button);
    }

    @Override
    public void resetValue() {
        super.resetValue();
        this.button.setValue(getValue());
    }

    public static <T> EnumEntry literal(JsonElement currentValue, Supplier<JsonElement> defaultValue, JsonElement[] values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback, Function<JsonElement, T> fromJSON) {
        return new EnumEntry(currentValue, defaultValue, List.of(values), jsonElement -> jsonElement.isJsonNull() ? Component.empty() : Component.literal(fromJSON.apply(jsonElement).toString()), restartRequired, name, tooltip, saveCallback);
    }

    public static <T> EnumEntry convertSchemaNames(Schema.Enum<T> schema, String translationKey, JsonElement currentValue, Supplier<JsonElement> defaultValue, List<T> values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        return new EnumEntry(currentValue, defaultValue, values.stream().map(t -> toJson(schema, t)).toList(), jsonElement -> jsonElement.isJsonNull() ? Component.empty() : Component.translatableWithFallback(translationKey + "." + jsonElement.getAsString(), convertToName(jsonElement.getAsString())), restartRequired, name, tooltip, saveCallback);
    }

    public static <T> JsonElement toJson(Schema.Enum<T> schema, T currentValue) {
        return new JsonPrimitive(schema.label().apply(currentValue));
    }


    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.button.active = isEditable();
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        this.button.setPosition(valueX, getPaddedY());
        this.button.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getValueWidth() {
        return 64;
    }
}
