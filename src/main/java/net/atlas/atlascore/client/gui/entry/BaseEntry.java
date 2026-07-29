package net.atlas.atlascore.client.gui.entry;

import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.Optional;

public abstract class BaseEntry extends ContainerObjectSelectionList.Entry<BaseEntry> {
    public abstract int bindOwner(ConfigCategory parent, ConfigScreen owner);
    public abstract void setEditable(boolean editable);
    public abstract void resetValueSafe();
    public abstract void setVisible(boolean visible);
    public abstract boolean isVisible();
    public abstract boolean isChanged();
    public abstract boolean save();
    public abstract Optional<Component> error();
    public abstract Optional<NarratableEntry> narratableForm();

    @Override
    public final void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        if (!this.isVisible()) return;
        this.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    public abstract void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a);
}
