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

    public static <T> EnumEntry convertSchemaNames(Schema.Enum<T> schema, JsonElement currentValue, Supplier<JsonElement> defaultValue, List<T> values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        return new EnumEntry(currentValue, defaultValue, values.stream().map(t -> toJson(schema, t)).toList(), jsonElement -> jsonElement.isJsonNull() ? Component.empty() : Component.translatableWithFallback("text.config." + jsonElement.getAsString(), snakeCaseToName(jsonElement.getAsString())), restartRequired, name, tooltip, saveCallback);
    }

    public static <T> JsonElement toJson(Schema.Enum<T> schema, T currentValue) {
        return new JsonPrimitive(schema.label().apply(currentValue));
    }

    public static String snakeCaseToName(String input) {
        List<Integer> capitalIndices = new ArrayList<>();
        capitalIndices.add(0);
        while (input.contains("_")) {
            int index = input.indexOf('_');
            if (index + 1 < input.length()) capitalIndices.add(index + 1);
            input = input.substring(0, index) + " " + input.substring(index + 1);
        }
        String[] output = {input};
        capitalIndices.forEach(capitalIndex -> {
            char original = output[0].charAt(capitalIndex);
            output[0] = output[0].substring(0, capitalIndex) + Character.toUpperCase(original) + (capitalIndex + 1 >= output[0].length() ? "" : output[0].substring(capitalIndex + 1));
        });
        return output[0];
    }


    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.button.active = isEditable();
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        this.button.setPosition(valueX, getPaddedY());
        this.button.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContent(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getValueWidth() {
        return 32;
    }
}
