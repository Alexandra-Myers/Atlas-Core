package net.atlas.atlascore.client.gui.entry;

import net.atlas.atlascore.client.gui.ConfigCategory;
import net.atlas.atlascore.client.gui.ConfigScreen;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public abstract class BaseEntry extends ContainerObjectSelectionList.Entry<BaseEntry> {
    public abstract void bindOwner(ConfigCategory parent, ConfigScreen owner);
    public abstract void setEditable(boolean editable);
    public abstract void resetValueSafe();
    public abstract boolean isChanged();
    public abstract boolean save();
    public abstract Optional<Component> error();
    public abstract NarratableEntry narratableForm();
}
