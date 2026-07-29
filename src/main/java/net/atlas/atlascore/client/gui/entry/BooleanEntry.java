package net.atlas.atlascore.client.gui.entry;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.util.ClientUtils;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class BooleanEntry extends ConfigEntry<Boolean> {
    public static final WidgetSprites ON = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_boolean_on"));
    public static final WidgetSprites OFF = ClientUtils.buildNoFocusedDisabled(AtlasCore.id("widget/config_boolean_off"));
    public static final Component TRUE = Component.translatableWithFallback("text.config.true", "True");
    public static final Component FALSE = Component.translatableWithFallback("text.config.false", "False");
    public final Button onButton;
    public final Button offButton;
    public BooleanEntry(Boolean currentValue, Supplier<Boolean> defaultValue, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Boolean> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.onButton = SpriteIconButton.builder(TRUE, button -> invert(), true)
                .sprite(ON, 20, 20)
                .size(20, 20).build();
        this.offButton = SpriteIconButton.builder(FALSE, button -> invert(), true)
                .sprite(OFF, 20, 20)
                .size(20, 20).build();
        this.addChild(this.onButton);
        this.addChild(this.offButton);
        if (currentValue == null || currentValue) this.offButton.visible = false;
        else this.onButton.visible = false;
    }

    @Override
    public void resetValue() {
        super.resetValue();
        Boolean currentValue = getValue();
        if (currentValue == null)  {
            this.onButton.visible = true;
            this.offButton.visible = false;
        } else {
            this.onButton.visible = currentValue;
            this.offButton.visible = !currentValue;
        }
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.onButton.active = isEditable();
        this.offButton.active = isEditable();
        int buttonX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        this.onButton.setPosition(buttonX, getPaddedY());
        this.offButton.setPosition(buttonX, getPaddedY());
        this.onButton.extractRenderState(graphics, mouseX, mouseY, a);
        this.offButton.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    public void invert() {
        if (this.getValue() != null) this.setValue(!this.getValue());
        else this.setValue(true);
        this.onButton.visible = !this.onButton.visible;
        this.offButton.visible = !this.offButton.visible;
    }
}
