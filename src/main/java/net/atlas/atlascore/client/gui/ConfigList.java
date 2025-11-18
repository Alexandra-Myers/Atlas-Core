package net.atlas.atlascore.client.gui;

import com.google.common.collect.ImmutableList;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ConfigList extends ContainerObjectSelectionList<ConfigList.Entry> {
    public ConfigList(Minecraft minecraft, int i, int j, int k, int l, int m) {
        super(minecraft, i, j, k, l, m);
        this.centerListVertically = false;
    }

    public void add(Button.Builder builder, @Nullable Button.Builder builder2) {
        this.addEntry(ConfigList.Entry.buttons(this.width, builder, builder2));
    }

    public void add(Button.Builder[] builders) {
        for(int i = 0; i < builders.length; i += 2) {
            this.add(builders[i], i < builders.length - 1 ? builders[i + 1] : null);
        }

    }

    public int getRowWidth() {
        return 400;
    }

    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 32;
    }

    @OnlyIn(Dist.CLIENT)
    protected static class Entry extends ContainerObjectSelectionList.Entry<Entry> {
        final List<AbstractWidget> children;

        private Entry(List<AbstractWidget> children) {
            this.children = ImmutableList.copyOf(children);
        }

        public static Entry buttons(int width, Button.Builder builder, @Nullable Button.Builder builder2) {
            AbstractWidget abstractWidget = builder.pos(width / 2 - 155, 0).size(150, 20).build();
            return builder2 == null ? new Entry(List.of(abstractWidget)) : new Entry(List.of(abstractWidget, builder2.pos(width / 2 + 5, 0).size(150, 20).build()));
        }

        public void render(GuiGraphics guiGraphics, int i, int j, int k, int l, int m, int n, int o, boolean bl, float f) {
            this.children.forEach((abstractWidget) -> {
                abstractWidget.setY(j);
                abstractWidget.render(guiGraphics, n, o, f);
            });
        }

        public List<? extends GuiEventListener> children() {
            return this.children;
        }

        public List<? extends NarratableEntry> narratables() {
            return this.children;
        }
    }
}