package net.atlas.atlascore.client.gui;

import net.atlas.atlascore.client.gui.entry.BaseEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.network.chat.Component;

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

    public static ConfigCategory create(Component name, ConfigScreen configScreen, int x, int y) {
        return new ConfigCategory(name, configScreen, configScreen.getMinecraft(), configScreen.width - x, 80, x, y, 24);
    }

    @Override
    public int addEntry(BaseEntry entry) {
        return super.addEntry(entry);
    }

    @Override
    public int addEntry(BaseEntry entry, int height) {
        entry.bindOwner(this, this.parent);
        int originalHeight = entry.getHeight();
        int index = super.addEntry(entry, height);
        entry.setX(getX());
        entry.setHeight(originalHeight);
        return index;
    }

    @Override
    public void addEntryToTop(BaseEntry entry) {
        super.addEntryToTop(entry);
    }

    @Override
    public void addEntryToTop(BaseEntry entry, int height) {
        entry.bindOwner(this, this.parent);
        List<BaseEntry> children = this.children();
        int originalHeight = entry.getHeight();
        super.addEntryToTop(entry, height);
        entry.setX(getX());
        entry.setHeight(originalHeight);
        for (BaseEntry child : children) {
            child.setX(0);
        }
    }

    public void emitReset() {
        this.children().forEach(BaseEntry::resetValueSafe);
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

    public void setEditable(boolean editable) {
        children().forEach(entry -> entry.setEditable(editable));
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
        repositionEntries();
    }

    public void repositionEntries() {
        int y = this.getY() + 2 - (int)this.scrollAmount();

        for(BaseEntry child : this.children()) {
            child.setY(y);
            y += child.getHeight();
            child.setX(this.getX());
        }
    }
}
