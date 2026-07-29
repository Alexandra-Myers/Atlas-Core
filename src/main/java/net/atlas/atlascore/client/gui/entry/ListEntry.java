package net.atlas.atlascore.client.gui.entry;

import com.google.gson.*;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.atlas.atlascore.util.ClientUtils;
import net.mehvahdjukaar.codecui.Schema;
import net.mehvahdjukaar.codecui.SchemaCodec;
import net.mehvahdjukaar.codecui.SchemaContext;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static net.atlas.atlascore.client.gui.entry.ObjectEntry.*;

public class ListEntry<T, A> extends ConfigEntry<JsonElement> {
    public static final WidgetSprites ADD_BUTTON = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_add_entry"));
    public static final Component ADD_ENTRY = Component.translatableWithFallback("text.config.add_entry", "Add Entry");
    public static final WidgetSprites REMOVE_BUTTON = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_remove_entry"));
    public static final Component REMOVE_ENTRY = Component.translatableWithFallback("text.config.remove_entry", "Remove Entry");
    public final Button expandButton;
    public final Button collapseButton;
    private final Either<Schema<A>, SchemaCodec<A>> schema;
    private final Schema<T> entrySchema;
    private final List<BaseEntry> subEntries = new ArrayList<>();
    private final ListAddEntry addEntry;
    private final String translationKey;
    private final int minSize;
    private final int maxSize;
    private boolean expanded;
    public ListEntry(Either<Schema<A>, SchemaCodec<A>> schema, Schema<T> entrySchema, String translationKey, int minSize, int maxSize, JsonElement currentValue, Supplier<JsonElement> defaultValue, boolean expanded, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<JsonElement> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.schema = schema;
        this.entrySchema = entrySchema;
        this.translationKey = translationKey;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.expanded = expanded;
        this.expandButton = SpriteIconButton.builder(EXPAND, button -> flip(), true)
                .sprite(COLLAPSED, 10, 10)
                .size(10, 10).build();
        this.collapseButton = SpriteIconButton.builder(COLLAPSE, button -> flip(), true)
                .sprite(EXPANDED, 10, 10)
                .size(10, 10).build();
        this.addChild(this.expandButton);
        this.addChild(this.collapseButton);
        this.subEntries.add(new SeparatorEntry(this.expanded, this.getX()));
        this.setInitialValue(wrapInputValue(currentValue));
        this.addEntry = new ListAddEntry();
        this.subEntries.add(this.addEntry);
        this.subEntries.add(new SeparatorEntry(this.expanded, this.getX()));
        if (this.expanded) this.expandButton.visible = false;
        else this.collapseButton.visible = false;
    }

    public JsonElement wrapInputValue(JsonElement value) {
        JsonArray currentEntries = enforceJsonArray(value);
        Optional<JsonArray> defaultEntries = Optional.of(this.defaultValue.get())
                .filter(JsonElement::isJsonArray)
                .map(JsonElement::getAsJsonArray);
        for (int i = 0; i < Math.max(minSize, Math.max(defaultEntries.map(JsonArray::size).orElse(-1), currentEntries.size())); i++) {
            JsonElement currentEntry = i >= currentEntries.size() ? JsonNull.INSTANCE : currentEntries.get(i);
            int finalIndex = i;
            JsonElement defaultEntry = defaultEntries
                    .map(resolvedDefaultEntries -> finalIndex >= resolvedDefaultEntries.size() ? JsonNull.INSTANCE :
                            resolvedDefaultEntries.get(finalIndex))
                    .orElse(JsonNull.INSTANCE);
            boolean isUnderTargetSize = i < minSize;
            if (!currentEntry.isJsonNull() || isUnderTargetSize) {
                this.subEntries.add(createConfigEntry(i + 1, currentEntry, defaultEntry));
                if (isUnderTargetSize) currentEntries.add(currentEntry);
            }
        }
        return currentEntries;
    }

    public void addSubEntry() {
        int index = this.subEntries.indexOf(this.addEntry);
        BaseEntry value = createConfigEntry(index, JsonNull.INSTANCE, JsonNull.INSTANCE);
        JsonArray writer = enforceJsonArray(this.getValue());
        writer.add(JsonNull.INSTANCE);
        setValue(writer);
        this.subEntries.add(index, value);
        if (this.owningCategory != null)
            this.owningCategory.addEntryBefore(this.addEntry, value);
    }

    public BaseEntry createConfigEntry(int index, JsonElement currentValue, JsonElement defaultValue) {
        ConfigEntry<?> ret = ConfigEntry.acceptBySchema(this.entrySchema, this.translationKey, currentValue, () -> defaultValue, this.restartRequired, encoded -> {
            JsonArray result = enforceJsonArray(this.getValue());
            result.set(index - 1, encoded);
            this.setValue(result);
        }).saveOnChange();
        Button removeButton = SpriteIconButton.builder(REMOVE_ENTRY, button -> {
                    if (this.subEntries.size() - 3 == this.minSize) return;
                    removeSubEntry(index);
                    JsonArray result = enforceJsonArray(this.getValue());
                    result.remove(index - 1);
                    this.setValue(result);
                }, true)
                .sprite(REMOVE_BUTTON, 20, 20)
                .size(20, 20).build();
        ret.setX(this.getX() + 10);
        return defaultValue.isJsonNull() ?
                ret.rebindResetButton(removeButton) : ret.addRemoveButton(removeButton);
    }

    public static JsonArray enforceJsonArray(JsonElement value) {
        return !value.isJsonArray() ? new JsonArray() : value.getAsJsonArray().deepCopy();
    }

    @Override
    public void resetValue() {
        super.resetValue();
        int lastIndex = this.subEntries.indexOf(this.addEntry);
        for (int i = Math.max(enforceJsonArray(this.getValue()).size(), this.minSize) + 1; i < lastIndex; i++) {
            removeSubEntry(i);
            i--;
            lastIndex--;
        }
        this.subEntries.forEach(BaseEntry::resetValueSafe);
    }

    public void removeSubEntry(int index) {
        BaseEntry removed = this.subEntries.remove(index);
        if (this.owningCategory != null)
            this.owningCategory.removeEntry(removed);
        removed.propagateRemoval();
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

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        this.expandButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.collapseButton.setPosition(this.getX() + 2, this.getY() + getBaseHeight() / 2 - 5);
        this.expandButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.collapseButton.extractRenderState(graphics, mouseX, mouseY, a);
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
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
        if (!this.getValue().isJsonArray()) return Optional.of(Component.literal("Not a JSON array: " + this.getValue()));
        return this.schema.right().map(codec -> {
                    DynamicOps<JsonElement> ops = SchemaContext.getRegistries().createSerializationContext(JsonOps.INSTANCE);
                    return codec.parse(ops, this.getValue());
                }).flatMap(DataResult::error)
                .<Component>map(e -> Component.literal("Invalid input: " + e)).or(() -> this.subEntries.stream()
                        .map(BaseEntry::error)
                        .flatMap(Optional::stream)
                        .findFirst());
    }

    public class ListAddEntry extends BaseEntry {
        public final Button addButton;
        public boolean visible;
        public ListAddEntry() {
            this.addButton = SpriteIconButton.builder(ADD_ENTRY, button -> ListEntry.this.addSubEntry(), true)
                    .sprite(ADD_BUTTON, 20, 20)
                    .size(20, 20).build();
            this.visible = ListEntry.this.expanded;
            this.setX(ListEntry.this.getX() + 10);
        }
        @Override
        public int bindOwner(ConfigCategory parent, ConfigScreen owner) {
            return 1;
        }

        @Override
        public void propagateRemoval() {

        }

        @Override
        public void setEditable(boolean editable) {

        }

        @Override
        public void resetValueSafe() {

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
        public boolean isChanged() {
            return false;
        }

        @Override
        public boolean save() {
            return false;
        }

        @Override
        public Optional<Component> error() {
            return Optional.empty();
        }

        @Override
        public Optional<NarratableEntry> narratableForm() {
            return Optional.empty();
        }

        @Override
        public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
            this.addButton.active = ListEntry.this.isEditable() && ListEntry.this.subEntries.size() - 3 < ListEntry.this.maxSize;
            this.addButton.setPosition(this.getX() + 10, this.getY());
            this.addButton.extractRenderState(graphics, mouseX, mouseY, a);
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of(this.addButton);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return List.of(this.addButton);
        }

        @Override
        public int getWidth() {
            return this.addButton.getWidth() + 10;
        }

        @Override
        public int getHeight() {
            return this.addButton.getHeight();
        }
    }
}
