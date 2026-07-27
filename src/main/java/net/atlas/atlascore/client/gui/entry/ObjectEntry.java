package net.atlas.atlascore.client.gui.entry;

import com.google.gson.JsonElement;
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
    private boolean expanded;
    private ConfigCategory category;
    public ObjectEntry(Either<Schema<T>, SchemaCodec<T>> schema, JsonElement currentValue, Supplier<JsonElement> defaultValue, boolean expanded, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.schema = schema;
        this.expanded = expanded;
        this.rawExtra = new EditBox(Minecraft.getInstance().font, 124, 20, EDIT_FIELDS);
        this.rawExtra.setMaxLength(999999);
        this.rawExtra.setValue("{}");
        this.expandButton = SpriteIconButton.builder(EXPAND, button -> flip(), true)
                .sprite(COLLAPSED, 10, 10)
                .size(10, 10).build();
        this.collapseButton = SpriteIconButton.builder(COLLAPSE, button -> flip(), true)
                .sprite(EXPANDED, 10, 10)
                .size(10, 10).build();
        this.addChild(this.rawExtra);
        this.addChild(this.expandButton);
        this.addChild(this.collapseButton);
        if (this.expanded) this.expandButton.visible = false;
        else this.collapseButton.visible = false;
    }

    @Override
    public void resetValue() {
        super.resetValue();
        if (this.category == null) return; // Unbound, somehow
        this.category.emitReset();
        JsonElement clearedOfNonExtras = getValue().deepCopy();
        if (clearedOfNonExtras.isJsonObject()) this.accountedKeys.forEach(key -> clearedOfNonExtras.getAsJsonObject().remove(key));
        this.rawExtra.setValue(CodecUI.GSON.toJson(clearedOfNonExtras));
    }

    @Override
    public void bindOwner(ConfigCategory parent, ConfigScreen owner) {
        super.bindOwner(parent, owner);
        this.category = ConfigCategory.create(this.name, owner, this.getX() + 10, this.getPaddedY() + getBaseHeight());
        this.category.visible = this.expanded;
        this.addChild(this.category);
        switch (this.schema.map(Function.identity(), SchemaCodec::schema)) {
            case Schema.Record<T> record -> record.fields().forEach(field -> this.bindRecordField(field, record));
            case Schema.Ref<T> recursive -> {
                Schema<?> target = recursive.target();
                if (target == null) return;
                switch (target) {
                    case Schema.Record<?> record -> record.fields().forEach(field -> this.bindRecordField(field, recursive));
                    default -> this.category.addEntry(ConfigEntry.accept(target, this.getValue(), this.defaultValue, null, this.restartRequired, null, this.tooltip, this.saveCallback));
                }
            }
            default -> {}
        }
        JsonElement clearedOfNonExtras = getValue().deepCopy();
        if (clearedOfNonExtras.isJsonObject()) this.accountedKeys.forEach(key -> clearedOfNonExtras.getAsJsonObject().remove(key));
        this.rawExtra.setValue(CodecUI.GSON.toJson(clearedOfNonExtras));
        this.rawExtra.setResponder(this::putAllFromString);
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
        this.category.addEntry(fieldEntry);
        extraFields.forEach(this.category::addEntry);
    }

    private BaseEntry entryOf(Schema<T> schema, String fieldName, Schema<?> source) {
        Supplier<ConfigEntry<?>> supplier = () -> {
            ConfigEntry<?> ret = ConfigEntry.acceptBySchema(source, fieldName, this.restartRequired, this.getValue(), this.defaultValue.get(), encoded -> {
                JsonObject result = this.getValue().getAsJsonObject().deepCopy();
                if (encoded.isJsonNull()) result.remove(fieldName);
                else result.add(fieldName, encoded);
                this.setValue(result);
            }).saveOnChange();
            ret.bindOwner(this.category, this.owner);
            return ret;
        };
        return source == schema ? new UnboundConfigEntry(supplier) : supplier.get();
    }

    public void putAllFromString(String str) {
        JsonReader reader = CodecUI.GSON.newJsonReader(new StringReader(str));
        try {
            JsonElement read = JsonParser.parseReader(reader);
            JsonElement value = getValue().deepCopy();
            if (value.isJsonObject() && read.isJsonObject()) {
                JsonObject readObject = read.getAsJsonObject();
                JsonObject object = value.getAsJsonObject();
                readObject.entrySet().forEach(entry -> object.add(entry.getKey(), entry.getValue()));
                this.setValue(value);
            }
        } catch (Exception ignored) {

        }
    }

    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        this.expandButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.collapseButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.expandButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.collapseButton.extractRenderState(graphics, mouseX, mouseY, a);
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.rawExtra.setEditable(isEditable());
        int buttonX = getX() + getWidth() - this.rawExtra.getWidth() - getResetWidth();
        this.rawExtra.setPosition(buttonX, getPaddedY());
        this.rawExtra.extractRenderState(graphics, mouseX, mouseY, a);
        if (isBound()) {
            this.category.setPosition(this.getX() + 10, this.getPaddedY() + getBaseHeight());
            this.category.setEditable(this.editable);
            this.category.extractRenderState(graphics, mouseX, mouseY, a);
        }
        super.extractContent(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getWidth() {
        int baseWidth = super.getWidth() + this.rawExtra.getWidth();
        return Math.min(isBound() ? this.category.getWidth() : baseWidth, baseWidth);
    }

    @Override
    public int getHeight() {
        return super.getHeight() + (this.expanded && isBound() ? this.category.getHeight() : 0) + 5;
    }

    public void flip() {
        this.expanded = !this.expanded;
        this.expandButton.visible = !this.expandButton.visible;
        this.collapseButton.visible = !this.collapseButton.visible;
        if (isBound()) {
            this.category.visible = this.expanded;
            this.owningCategory.repositionEntries();
        }
    }

    public final boolean isBound() {
        return this.category != null;
    }

    @Override
    public Optional<Component> error() {
        return this.schema.right().map(codec -> {
            DynamicOps<JsonElement> ops = SchemaContext.getRegistries().createSerializationContext(JsonOps.INSTANCE);
            return codec.parse(ops, this.getValue());
        }).flatMap(DataResult::error)
                .<Component>map(e -> Component.literal("Invalid input: " + e)).or(super::error);
    }
}
