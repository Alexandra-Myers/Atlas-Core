package net.atlas.atlascore.client.gui.entry;

import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static net.atlas.atlascore.client.gui.entry.ListEntry.ADD_BUTTON;
import static net.atlas.atlascore.client.gui.entry.ListEntry.ADD_ENTRY;

public class UnboundConfigEntry extends BaseEntry {
    public final Supplier<ConfigEntry<?>> entrySupplier;
    public final Button createButton;
    public final NarratableUnboundConfigEntry narratableForm;
    public List<? extends GuiEventListener> children;
    public List<? extends NarratableEntry> narratables;
    public @Nullable ConfigEntry<?> entry = null;
    public ConfigScreen owner = null;
    public UnboundConfigEntry(Supplier<ConfigEntry<?>> entrySupplier) {
        this.entrySupplier = entrySupplier;
        this.createButton = SpriteIconButton.builder(ADD_ENTRY, button -> bind(), true)
                .sprite(ADD_BUTTON, 20, 20)
                .size(20, 20).build();
        this.children = List.of(this.createButton);
        this.narratables = List.of(this.createButton);
        this.narratableForm = new NarratableUnboundConfigEntry();
    }

    public void bind() {
        this.entry = this.entrySupplier.get();
        this.children = List.of(this.entry);
        this.narratables = this.entry.narratables();
        this.createButton.visible = false;
    }

    @Override
    public void bindOwner(ConfigCategory parent, ConfigScreen owner) {
        this.owner = owner;
    }

    @Override
    public void setEditable(boolean editable) {
        if (this.entry != null) this.entry.setEditable(editable);
    }

    @Override
    public void resetValueSafe() {
        if (this.entry != null) this.entry.resetValueSafe();
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) this.createButton.visible = false;
        else this.createButton.visible = this.entry == null;
        if (this.entry != null) this.entry.setVisible(visible);
    }

    @Override
    public boolean isVisible() {
        return this.entry == null ? this.createButton.visible : this.entry.isVisible();
    }

    @Override
    public boolean isChanged() {
        return this.entry != null && this.entry.isChanged();
    }

    @Override
    public boolean save() {
        return this.entry != null && this.entry.save();
    }

    @Override
    public Optional<Component> error() {
        return Optional.ofNullable(this.entry).flatMap(ConfigEntry::error);
    }

    @Override
    public Optional<NarratableEntry> narratableForm() {
        return Optional.ofNullable(this.narratableForm);
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        this.createButton.setPosition(this.getX() + 10, this.getY());
        this.createButton.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.entry != null) {
            this.entry.setPosition(this.getX(), this.getY());
            this.entry.extractContent(graphics, mouseX, mouseY, hovered, a);
        }
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return this.children;
    }

    @Override
    public @NonNull List<? extends NarratableEntry> narratables() {
        return this.narratables;
    }

    public final class NarratableUnboundConfigEntry implements NarratableEntry {
        @Override
        public @NonNull NarrationPriority narrationPriority() {
            if (UnboundConfigEntry.this.isFocused()) return NarrationPriority.FOCUSED;
            else return NarrationPriority.NONE;
        }

        @Override
        public void updateNarration(@NonNull NarrationElementOutput output) {
            this.getNarratables().forEach(entry -> entry.updateNarration(output));
        }

        @Override
        public @NonNull Collection<? extends NarratableEntry> getNarratables() {
            return UnboundConfigEntry.this.narratables();
        }
    }
}
