package net.atlas.atlascore.client.gui.entry.textlike.color;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ColorDisplayWidget extends AbstractWidget {
    private final Supplier<Boolean> isFocused;
    private final Supplier<Integer> colorGetter;
    protected int size;

    public ColorDisplayWidget(int x, int y, int size, Supplier<Boolean> isFocused, Supplier<Integer> colorGetter) {
        super(x, y, size, size, Component.empty());
        this.size = size;
        this.isFocused = isFocused;
        this.colorGetter = colorGetter;
    }

    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(this.getX(), this.getY(), this.getX() + this.size, this.getY() + this.size, this.isFocused.get() ? -1 : 0xFFA0A0A0);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.size - 1, this.getY() + this.size - 1, -1);
        graphics.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.size - 1, this.getY() + this.size - 1, this.colorGetter.get());
    }

    public void onClick(MouseButtonEvent event, boolean doubleClick) {
    }

    public void onRelease(MouseButtonEvent event) {
    }

    public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}