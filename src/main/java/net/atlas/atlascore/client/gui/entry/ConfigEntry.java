package net.atlas.atlascore.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.atlas.atlascore.client.gui.elements.BoundNarratablesList;
import net.atlas.atlascore.client.gui.entry.textlike.color.ColorEntry;
import net.atlas.atlascore.client.gui.entry.number.DoubleRangeEntry;
import net.atlas.atlascore.client.gui.entry.number.FloatRangeEntry;
import net.atlas.atlascore.client.gui.entry.number.IntRangeEntry;
import net.atlas.atlascore.client.gui.entry.number.LongRangeEntry;
import net.atlas.atlascore.client.gui.entry.textlike.string.CodecBackedStringEntry;
import net.atlas.atlascore.client.gui.entry.textlike.string.StringEntry;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.util.ClientUtils;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaContext;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.atlas.atlascore.client.gui.entry.EnumEntry.snakeCaseToName;

public abstract class ConfigEntry<T> extends BaseEntry {
    public static final WidgetSprites RESET_SPRITE = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_reset"));
    public static final Component RESET = Component.translatableWithFallback("text.config.reset", "Reset");
    public T value;
    public boolean visible = true;
    public boolean editable = true;
    public boolean optional = false;
    public boolean saveOnChange = false;
    public ConfigCategory owningCategory;
    public ConfigScreen owner;
    public int x;
    public int y;
    private Button resetButton;
    private @Nullable Button removeButton;
    public final T initialValue;
    public final Supplier<T> defaultValue;
    public final boolean restartRequired;
    public final @Nullable Component name;
    public final Supplier<Optional<Component[]>> tooltip;
    public final Consumer<T> saveCallback;
    private final List<GuiEventListener> children = new ArrayList<>();
    private final List<NarratableEntry> narratables = new ArrayList<>();
    private final NarratableEntry narratableForm = new BoundNarratablesList(() -> {
        if (this.isFocused()) return NarratableEntry.NarrationPriority.FOCUSED;
        else return NarratableEntry.NarrationPriority.NONE;
    }, this.narratables);

    public ConfigEntry(T currentValue, Supplier<T> defaultValue, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<T> saveCallback) {
        super();
        this.value = currentValue;
        this.initialValue = currentValue;
        this.defaultValue = defaultValue;
        this.restartRequired = restartRequired;
        this.name = name;
        this.tooltip = tooltip;
        this.saveCallback = saveCallback;
        this.resetButton = SpriteIconButton.builder(RESET, button -> resetValue(), true)
                .sprite(RESET_SPRITE, 20, 20)
                .size(20, 20).build();
        this.addChild(this.resetButton);
        this.narratables.add(new NarratableConfigEntry());
    }

    public ConfigEntry<?> addRemoveButton(Button removeButton) {
        if (removeButton == null) return this;
        this.addChild(1, removeButton);
        this.removeButton = removeButton;
        return this;
    }

    public ConfigEntry<?> rebindResetButton(Button resetOverride) {
        this.removeChild(this.resetButton);
        this.addChild(0, resetOverride);
        this.resetButton = resetOverride;
        return this;
    }

    public void removeChild(AbstractWidget widget) {
        this.children.remove(widget);
        this.narratables.remove(widget);
    }

    public void addChild(AbstractWidget widget) {
        this.children.add(widget);
        this.narratables.add(widget);
    }

    public void addChild(int index, AbstractWidget widget) {
        this.children.add(index, widget);
        this.narratables.add(index, widget);
    }

    public void addChild(BaseEntry entry) {
        this.children.add(entry);
        entry.narratableForm().ifPresent(this.narratables::add);
    }

    public ConfigEntry<T> saveOnChange() {
        this.saveOnChange = true;
        return this;
    }

    public static <T> ConfigEntry<?> acceptBySchema(SchemaCodec<T> schema, AtlasConfig.ConfigHolder<T> tConfigHolder) {
        final boolean usesRange = tConfigHolder.heldValue.isRange() || tConfigHolder.heldValue.possibleValues() == null;
        T[] values = tConfigHolder.heldValue.possibleValues();
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        Function<T, JsonElement> encode = raw -> raw == null ? JsonNull.INSTANCE : schema.encodeStart(SchemaContext.getRegistries().createSerializationContext(ops), raw).getOrThrow();
        Function<JsonElement, T> decode = jsonElement -> jsonElement.isJsonNull() ? null : schema.parse(SchemaContext.getRegistries().createSerializationContext(ops), jsonElement).getOrThrow();
        JsonElement[] encodedValues = new JsonElement[0];
        if (values != null) {
            encodedValues = new JsonElement[values.length];
            for (int i = 0; i < values.length; i++) {
                encodedValues[i] = encode.apply(values[i]);
            }
        }
        return accept(Either.right(schema), usesRange, tConfigHolder.isSlider(), encode.apply(tConfigHolder.get()), () -> encode.apply(tConfigHolder.heldValue.defaultValue()), encodedValues, tConfigHolder.restartRequired.restartRequiredOnClient(), Component.translatable(tConfigHolder.getTranslationKey()), tConfigHolder.tooltip, encoded -> tConfigHolder.setValue(decode.apply(encoded)));
    }

    public static <T> ConfigEntry<?> acceptBySchema(Schema<T> schema, String name, boolean restartRequired, JsonElement value, JsonElement defaultValue, Consumer<JsonElement> saveCallback) {
        return accept(schema, Optional.ofNullable(value.getAsJsonObject().get(name)).orElse(JsonNull.INSTANCE), () -> Optional.ofNullable(defaultValue.getAsJsonObject().get(name)).orElse(JsonNull.INSTANCE), new JsonElement[0], restartRequired, Component.literal(snakeCaseToName(name)), Optional::empty, saveCallback);
    }

    public static <T> ConfigEntry<?> acceptBySchema(Schema<T> schema, JsonElement value, Supplier<JsonElement> defaultValue, boolean restartRequired, Consumer<JsonElement> saveCallback) {
        return accept(schema, value, defaultValue, new JsonElement[0], restartRequired, null, Optional::empty, saveCallback);
    }

    public static <T> ConfigEntry<?> accept(Schema<T> schema, JsonElement currentValue, Supplier<JsonElement> defaultValue, JsonElement[] values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        return accept(Either.left(schema), currentValue, defaultValue, values, restartRequired, name, tooltip, saveCallback);
    }

    public static <T> ConfigEntry<?> accept(Either<Schema<T>, SchemaCodec<T>> schema, JsonElement currentValue, Supplier<JsonElement> defaultValue, JsonElement[] values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        return accept(schema, true, false, currentValue, defaultValue, values, restartRequired, name, tooltip, saveCallback);
    }

    public static <T> ConfigEntry<?> accept(Either<Schema<T>, SchemaCodec<T>> schema, boolean usesRange, boolean isSlider, JsonElement currentValue, Supplier<JsonElement> defaultValue, JsonElement[] values, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        return switch (schema.map(Function.identity(), SchemaCodec::schema)) {
            case Schema.Record<T> ignored -> new ObjectEntry<>(schema, currentValue, defaultValue, schema.map(s -> false, c -> true), restartRequired, name, tooltip, saveCallback);
            case Schema.Ref<T> ignored -> new ObjectEntry<>(schema, currentValue, defaultValue, schema.map(s -> false, c -> true), restartRequired, name, tooltip, saveCallback);
            case Schema.ListOf<?> list -> new ListEntry<>(list.element(), list.min(), list.max(), currentValue, defaultValue, schema.map(s -> false, c -> true), restartRequired, name, tooltip, saveCallback);
            case Schema.Str str -> usesRange ?
                    new StringEntry(mapNullableJsonElement(currentValue, JsonElement::getAsString),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsString),
                            str.minLen(),
                            str.maxLen(),
                            str.pattern(),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)))
                    : EnumEntry.literal(currentValue,
                    defaultValue,
                    values,
                    restartRequired,
                    name, tooltip,
                    saveCallback,
                    element -> mapNullableJsonElement(element, JsonElement::getAsString));
            case Schema.Enum<T> enumSchema -> EnumEntry.convertSchemaNames(enumSchema,
                    currentValue,
                    defaultValue,
                    enumSchema.options(),
                    restartRequired,
                    name,
                    tooltip,
                    saveCallback);
            case Schema.IntRange range -> usesRange ?
                    IntRangeEntry.accept(isSlider, mapNullableJsonElement(currentValue, JsonElement::getAsInt),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsInt),
                            range.min(),
                            range.max(),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)))
                    : EnumEntry.literal(currentValue,
                    defaultValue,
                    values,
                    restartRequired,
                    name, tooltip,
                    saveCallback,
                    element -> mapNullableJsonElement(element, JsonElement::getAsInt));
            case Schema.Color color -> new ColorEntry(mapNullableJsonElement(currentValue, jsonElement -> AtlasConfig.getColor(jsonElement.getAsString(), -1, color.hasAlpha())),
                    () -> mapNullableJsonElement(defaultValue.get(), jsonElement -> AtlasConfig.getColor(jsonElement.getAsString(), -1, color.hasAlpha())),
                    color.hasAlpha(),
                    restartRequired,
                    name,
                    tooltip,
                    input -> saveCallback.accept(new JsonPrimitive(AtlasConfig.ColorHolder.toColorHex(color.hasAlpha(), input))));
            case Schema.LongRange range -> usesRange ?
                    new LongRangeEntry(mapNullableJsonElement(currentValue, JsonElement::getAsLong),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsLong),
                            range.min(),
                            range.max(),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)))
                    : EnumEntry.literal(currentValue,
                    defaultValue,
                    values,
                    restartRequired,
                    name,
                    tooltip,
                    saveCallback,
                    element -> mapNullableJsonElement(element, JsonElement::getAsLong));
            case Schema.DoubleRange range -> usesRange ?
                    new DoubleRangeEntry(mapNullableJsonElement(currentValue, JsonElement::getAsDouble),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsDouble),
                            range.min(),
                            range.max(),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)))
                    : EnumEntry.literal(currentValue,
                    defaultValue,
                    values,
                    restartRequired,
                    name,
                    tooltip,
                    saveCallback,
                    element -> mapNullableJsonElement(element, JsonElement::getAsDouble));
            case Schema.FloatRange range -> usesRange ?
                    new FloatRangeEntry(mapNullableJsonElement(currentValue, JsonElement::getAsFloat),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsFloat),
                            range.min(),
                            range.max(),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)))
                    : EnumEntry.literal(currentValue,
                    defaultValue,
                    values,
                    restartRequired,
                    name,
                    tooltip,
                    saveCallback,
                    element -> mapNullableJsonElement(element, JsonElement::getAsFloat));
            case Schema.Bool ignored ->
                    new BooleanEntry(mapNullableJsonElement(currentValue, JsonElement::getAsBoolean),
                            () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsBoolean),
                            restartRequired,
                            name,
                            tooltip,
                            input -> saveCallback.accept(new JsonPrimitive(input)));
            case null, default -> new CodecBackedStringEntry<>(schema,
                    mapNullableJsonElement(currentValue, JsonElement::getAsString),
                    () -> mapNullableJsonElement(defaultValue.get(), JsonElement::getAsString),
                    0,
                    Integer.MAX_VALUE,
                    null,
                    restartRequired,
                    name,
                    tooltip,
                    input -> saveCallback.accept(new JsonPrimitive(input)));
        };
    }

    private static <T> @Nullable T mapNullableJsonElement(JsonElement jsonElement, Function<JsonElement, T> toTFunc) {
        return jsonElement.isJsonNull() ? null : toTFunc.apply(jsonElement);
    }

    public T getValue() {
        return this.value;
    }

    public void setValue(T newValue) {
        this.value = newValue;
        if (this.saveOnChange && isChanged()) this.saveCallback.accept(this.value);
    }

    public void resetValue() {
        setValue(this.defaultValue.get());
    }

    @Override
    public void resetValueSafe() {
        boolean temp = this.saveOnChange;
        this.saveOnChange = false;
        resetValue();
        this.saveOnChange = temp;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isVisible() {
        return this.visible;
    }

    @Override
    public void bindOwner(ConfigCategory parent, ConfigScreen owner) {
        this.owningCategory = parent;
        this.owner = owner;
    }

    public void setServerManaged(boolean serverManaged) {
        this.editable &= !serverManaged;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public boolean isEditable() {
        return this.owner != null && this.editable;
    }

    public boolean isChanged() {
        return !Objects.equals(this.getValue(), this.initialValue);
    }

    public boolean save() {
        if (!isChanged()) return false;
        this.saveCallback.accept(this.getValue());
        return this.restartRequired;
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        int buttonX = getX() + getWidth() - getResetWidth();
        this.resetButton.active = isEditable() && !Objects.equals(this.defaultValue.get(), this.getValue());
        this.resetButton.setPosition(buttonX, getPaddedY());
        this.resetButton.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.removeButton == null) return;
        buttonX += this.resetButton.getWidth() + 2;
        this.removeButton.active = isEditable();
        this.removeButton.setPosition(buttonX, getPaddedY());
        this.removeButton.extractRenderState(graphics, mouseX, mouseY, a);
    }

    public void extractNameAndTooltip(GuiGraphicsExtractor graphics, boolean hovered, int inset, int mouseX, int mouseY) {
        int left = getX() + getTextInset();
        int top = getY() + inset;
        int bottom = getY() + getBaseHeight() - inset;
        Optional<Component> error = this.error();
        if (this.name != null) {
            MutableComponent name = this.name.copy();
            if (error.isPresent()) name.withStyle(ChatFormatting.RED, ChatFormatting.ITALIC);
            graphics.textRenderer().acceptScrollingWithDefaultCenter(name, left, left + getTextWidth(), top, bottom);
        }
        List<Component> tooltipLines = new ArrayList<>();
        this.tooltip.get().ifPresent(lines -> tooltipLines.addAll(List.of(lines)));
        error.ifPresent(tooltipLines::add);
        if ((hovered || error.isPresent()) && !tooltipLines.isEmpty() && this.owner != null) graphics.setComponentTooltipForNextFrame(this.owner.getFont(), tooltipLines, mouseX, mouseY);
    }

    @Override
    public void setX(int x) {
        this.x = x;
    }

    @Override
    public void setY(int y) {
        this.y = y;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    public int getPaddedY() {
        return getY() + getHeightPadding() / 2;
    }

    public int getTextInset() {
        return 10;
    }

    public int getTextWidth() {
        if (this.name == null) return 0;
        return 120;
    }

    public int getValueWidth() {
        return 20;
    }

    public final int getResetWidth() {
        return this.resetButton.getWidth() + (this.removeButton != null ? this.removeButton.getWidth() + 2 : 0);
    }

    @Override
    public int getWidth() {
        return getTextInset() + getTextWidth() + getValueWidth() + getResetWidth() + 10;
    }

    @Override
    public int getHeight() {
        return getBaseHeight();
    }

    public int getBaseHeight() {
        return getUnpaddedBaseHeight() + getHeightPadding();
    }

    public int getUnpaddedBaseHeight() {
        return 20;
    }

    public int getHeightPadding() {
        return 10;
    }

    public Optional<Component> error() {
        return Optional.empty();
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public @NonNull List<? extends NarratableEntry> narratables() {
        return this.narratables;
    }

    @Override
    public Optional<NarratableEntry> narratableForm() {
        return Optional.of(this.narratableForm);
    }

    public final class NarratableConfigEntry implements NarratableEntry {
        @Override
        public @NonNull NarrationPriority narrationPriority() {
            if (ConfigEntry.this.isFocused()) return NarrationPriority.FOCUSED;
            else return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NonNull NarrationElementOutput output) {
            if (ConfigEntry.this.name != null) output.add(NarratedElementType.TITLE, ConfigEntry.this.name);
            Optional<Component[]> tooltip = ConfigEntry.this.tooltip.get();
            tooltip.ifPresent(lines -> output.add(NarratedElementType.HINT, lines));
        }
    }
}
