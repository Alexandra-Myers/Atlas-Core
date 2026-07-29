package net.atlas.atlascore.client.gui.entry;

import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Optional;

public class SeparatorEntry extends BaseEntry {
    public final int inset;
    public boolean visible;
    public SeparatorEntry(boolean visible, int x) {
        this(visible, 7, x);
    }
    public SeparatorEntry(boolean visible, int inset, int x) {
        this.visible = visible;
        this.inset = inset;
        this.setX(x);
    }
    @Override
    public int bindOwner(ConfigCategory parent, ConfigScreen owner) {
        this.setWidth(parent.getWidth() - (this.inset * 2) - getX());
        this.setHeight(5);
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
    public @NonNull List<? extends NarratableEntry> narratables() {
        return List.of();
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        graphics.horizontalLine(this.getX() + this.inset, this.getX() + this.getWidth(), this.getY() + 2, 0xFFA0A0A0);
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        return List.of();
    }
}
