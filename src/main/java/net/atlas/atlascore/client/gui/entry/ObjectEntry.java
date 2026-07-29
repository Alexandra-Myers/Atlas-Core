package net.atlas.atlascore.client.gui.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.mehvahdjukaar.codecui.CodecUI;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.atlas.atlascore.client.gui.entry.textlike.TextLikeEntry.EDIT_FIELDS;
import static net.atlas.atlascore.util.StringUtils.convertCamelCaseToSnakeCase;

public class ObjectEntry<T> extends ConfigEntry<JsonElement> {
    public static final Identifier COLLAPSED = AtlasCore.id("widget/config_collapsed");
    public static final Identifier EXPANDED = AtlasCore.id("widget/config_expanded");
    public static final Component EXPAND = Component.translatableWithFallback("text.config.expand", "Expand");
    public static final Component COLLAPSE = Component.translatableWithFallback("text.config.collapse", "Collapse");
    public final Button expandButton;
    public final Button collapseButton;
    private final EditBox rawExtra;
    private final Either<Schema<T>, SchemaCodec<T>> schema;
    private final List<String> accountedKeys = new ArrayList<>();
    private final List<BaseEntry> subEntries = new ArrayList<>();
    private final String translationKey;
    private boolean expanded;
    public ObjectEntry(Either<Schema<T>, SchemaCodec<T>> schema, String translationKey, JsonElement currentValue, Supplier<JsonElement> defaultValue, boolean expanded, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.schema = schema;
        this.translationKey = translationKey;
        this.expanded = expanded;
        this.rawExtra = new EditBox(Minecraft.getInstance().font, 124, 20, EDIT_FIELDS);
        this.rawExtra.setMaxLength(999999);
        this.rawExtra.setValue("{}");
        this.rawExtra.setResponder(this::putAllFromString);
        this.expandButton = SpriteIconButton.builder(EXPAND, button -> flip(), true)
                .sprite(COLLAPSED, 10, 10)
                .size(10, 10).build();
        this.collapseButton = SpriteIconButton.builder(COLLAPSE, button -> flip(), true)
                .sprite(EXPANDED, 10, 10)
                .size(10, 10).build();
        this.addChild(this.rawExtra);
        this.addChild(this.expandButton);
        this.addChild(this.collapseButton);
        this.subEntries.add(new SeparatorEntry(this.expanded, this.getX()));
        switch (this.schema.map(Function.identity(), SchemaCodec::schema)) {
            case Schema.Record<T> record -> record.fields().forEach(field -> this.bindRecordField(field, record));
            case Schema.Ref<T> recursive -> {
                Schema<?> target = recursive.target();
                if (target == null) return;
                switch (target) {
                    case Schema.Record<?> record -> record.fields().forEach(field -> this.bindRecordField(field, recursive));
                    default -> this.subEntries.add(ConfigEntry.accept(target, translationKey, this.getValue(), this.defaultValue, null, this.restartRequired, null, this.tooltip, this.saveCallback));
                }
            }
            default -> {}
        }
        this.bindRawExtraToValue();
        this.subEntries.add(new SeparatorEntry(this.expanded, this.getX()));
        if (this.expanded) this.expandButton.visible = false;
        else this.collapseButton.visible = false;
    }

    @Override
    public void resetValue() {
        super.resetValue();
        this.subEntries.forEach(BaseEntry::resetValueSafe);
        this.bindRawExtraToValue();
    }

    public void removeSubEntry(int index) {
        BaseEntry removed = this.subEntries.remove(index);
        if (this.owningCategory != null)
            this.owningCategory.removeEntry(removed);
        removed.propagateRemoval();
    }

    public void bindRawExtraToValue() {
        JsonObject clearedOfNonExtras = enforceJsonObject(getValue());
        this.accountedKeys.forEach(clearedOfNonExtras::remove);
        this.rawExtra.setValue(CodecUI.GSON.toJson(clearedOfNonExtras));
    }

    @Override
    public int bindOwner(ConfigCategory parent, ConfigScreen owner) {
        int indices = super.bindOwner(parent, owner);
        parent.addEntriesAfter(this, this.subEntries);
        indices += this.subEntries.size();
        return indices;
    }

    @Override
    public void propagateRemoval() {
        super.propagateRemoval();
        int size = this.subEntries.size();
        for (int i = 0; i < size; i++) {
            removeSubEntry(i);
            i--;
            size--;
        }
    }

    public void bindRecordField(Schema.Field<?, ?> field, Schema<T> schema) {
        Schema<?> source = field.schema();
        String name = field.name();
        List<BaseEntry> extraFields = List.of();
        if (source instanceof Schema.OneOf<?> one) { // Merge inwards
            name = one.typeField();
            if (one.valueField() != null) {
                String valueField = one.valueField();
                BaseEntry fieldEntry = entryOf(schema, valueField, new Schema.Opaque<>(null, null));
                extraFields = List.of(fieldEntry);
                this.accountedKeys.add(valueField);
            }
        }
        String finalName = name;
        this.accountedKeys.add(finalName);
        BaseEntry fieldEntry = entryOf(schema, finalName, source);
        this.subEntries.add(fieldEntry);
        this.subEntries.addAll(extraFields);
    }

    private BaseEntry entryOf(Schema<T> schema, String fieldName, Schema<?> source) {
        Supplier<ConfigEntry<?>> supplier = () -> {
            ConfigEntry<?> ret = ConfigEntry.acceptBySchema(source, this.translationKey + "." + convertCamelCaseToSnakeCase(fieldName), fieldName, this.restartRequired,
                    readOptional(this.getValue(), fieldName),
                    () -> readOptional(this.defaultValue.get(), fieldName),
                    encoded -> {
                JsonObject result = enforceJsonObject(this.getValue());
                if (encoded.isJsonNull()) result.remove(fieldName);
                else result.add(fieldName, encoded);
                this.setValue(result);
            }).saveOnChange();
            if (this.owningCategory != null) this.owningCategory.addEntryAfter(this.subEntries.get(this.subEntries.size() - 2), ret);
            return ret;
        };
        BaseEntry ret = source == schema ? new UnboundConfigEntry(supplier) : supplier.get();
        ret.setX(this.getX() + 10);
        ret.setVisible(this.expanded);
        return ret;
    }

    public static JsonObject enforceJsonObject(JsonElement value) {
        return !value.isJsonObject() ? new JsonObject() : value.getAsJsonObject().deepCopy();
    }

    public static JsonElement readOptional(JsonElement source, String name) {
        return !source.isJsonObject() ? JsonNull.INSTANCE : Optional.ofNullable(source.getAsJsonObject().get(name))
                .orElse(JsonNull.INSTANCE);
    }

    public void putAllFromString(String str) {
        JsonReader reader = CodecUI.GSON.newJsonReader(new StringReader(str));
        try {
            JsonElement read = JsonParser.parseReader(reader);
            JsonObject value = enforceJsonObject(getValue());
            if (read.isJsonObject()) {
                JsonObject readObject = read.getAsJsonObject();
                readObject.entrySet().forEach(entry -> value.add(entry.getKey(), entry.getValue()));
                this.setValue(value);
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        this.expandButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.collapseButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.expandButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.collapseButton.extractRenderState(graphics, mouseX, mouseY, a);
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.rawExtra.setEditable(isEditable());
        int buttonX = getX() + getWidth() - this.rawExtra.getWidth() - getResetWidth();
        this.rawExtra.setPosition(buttonX, getPaddedY());
        this.rawExtra.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getWidth() {
        return super.getWidth() + this.rawExtra.getWidth();
    }

    public void flip() {
        this.expanded = !this.expanded;
        this.expandButton.visible = !this.expandButton.visible;
        this.collapseButton.visible = !this.collapseButton.visible;
        this.subEntries.forEach(baseEntry -> baseEntry.setVisible(this.expanded));
        this.owningCategory.atlas_core$repositionEntries();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        this.subEntries.forEach(entry -> entry.setX(x + 10));
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        this.subEntries.forEach(baseEntry -> baseEntry.setVisible(visible));
    }

    @Override
    public Optional<Component> error() {
        if (!this.getValue().isJsonObject()) return Optional.of(Component.literal("Not a JSON object: " + this.getValue()));
        return this.schema.right().map(codec -> {
            DynamicOps<JsonElement> ops = SchemaContext.getRegistries().createSerializationContext(JsonOps.INSTANCE);
            return codec.parse(ops, this.getValue());
        }).flatMap(DataResult::error)
                .<Component>map(e -> Component.literal("Invalid input: " + e)).or(super::error);
    }
}
