package net.atlas.atlascore.client.gui;

import net.atlas.atlascore.client.gui.entry.BaseEntry;
import net.atlas.atlascore.mixin.AbstractSelectionListAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ConfigCategory extends ContainerObjectSelectionList<BaseEntry> {
    private final ConfigScreen parent;
    public final Component name;

    public ConfigCategory(Component name, ConfigScreen parent, Minecraft minecraft, int width, int height, int x, int y, int itemHeight) {
        super(minecraft, width, height, y, itemHeight);
        this.name = name;
        this.parent = parent;
        this.setX(x);
    }

    public static ConfigCategory create(Component name, ConfigScreen configScreen) {
        return new ConfigCategory(name, configScreen, configScreen.getMinecraft(), configScreen.width, configScreen.height - 123, 0, 63, 24);
    }

    public void addEntryBefore(BaseEntry target, BaseEntry entry) {
        int index = this.children().indexOf(target);
        double scrollFromBottom = (double)this.maxScrollAmount() - this.scrollAmount();
        //noinspection unchecked
        ((AbstractSelectionListAccessor<BaseEntry>)this).getChildren().add(index, entry);
        this.setScrollAmount((double)this.maxScrollAmount() - scrollFromBottom);
        entry.bindOwner(this, this.parent);
    }

    @Override
    public int addEntry(BaseEntry entry) {
        return super.addEntry(entry);
    }

    @Override
    public int addEntry(BaseEntry entry, int height) {
        int originalX = entry.getX();
        int originalHeight = entry.getHeight();
        int index = super.addEntry(entry, height);
        entry.setX(getX() + originalX);
        entry.setHeight(originalHeight);
        entry.bindOwner(this, this.parent);
        return index;
    }

    @Override
    public void addEntryToTop(BaseEntry entry) {
        super.addEntryToTop(entry);
    }

    @Override
    public void addEntryToTop(BaseEntry entry, int height) {
        int originalX = entry.getX();
        int originalHeight = entry.getHeight();
        super.addEntryToTop(entry, height);
        entry.setX(getX() + originalX);
        entry.setHeight(originalHeight);
        entry.bindOwner(this, this.parent);
    }

    @Override
    public void removeEntries(@NonNull List<BaseEntry> entries) {
        super.removeEntries(entries);
    }

    @Override
    public void removeEntry(BaseEntry entry) {
        super.removeEntry(entry);
    }

    @Override
    public void removeEntryFromTop(BaseEntry entry) {
        super.removeEntryFromTop(entry);
    }

    public boolean hasErrors() {
        for (BaseEntry entry : children()) {
            if (entry.error().isPresent()) return true;
        }
        return false;
    }

    public boolean isChanged() {
        for (BaseEntry entry : children()) {
            if (entry.isChanged()) return true;
        }
        return false;
    }

    public boolean save() {
        boolean isRestartRequired = false;
        for (BaseEntry entry : children()) {
            isRestartRequired |= entry.save();
        }
        return isRestartRequired;
    }

    @Override
    public int getRowLeft() {
        return getX();
    }

    @Override
    public int getRowWidth() {
        return getWidth();
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        setWidth(this.parent.width - x);
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        this.atlas_core$repositionEntries();
    }

    public void atlas_core$repositionEntries() {
        int y = this.getY() + 2 - (int)this.scrollAmount();

        for(BaseEntry child : this.children()) {
            if (!child.isVisible()) continue;
            child.setY(y);
            y += child.getHeight();
        }
    }
}
