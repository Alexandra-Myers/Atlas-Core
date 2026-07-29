package net.atlas.atlascore.client.gui.entry.textlike;

import com.mojang.serialization.DataResult;
import net.atlas.atlascore.client.gui.entry.ConfigEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class TextLikeEntry<T> extends ConfigEntry<T> {
    public static final Component EDIT_FIELDS = Component.translatableWithFallback("text.config.edit", "Edit Field");
    public final EditBox editBox;
    public Component error = null;
    public TextLikeEntry(T currentValue, Supplier<T> defaultValue, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<T> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.editBox = new EditBox(Minecraft.getInstance().font, 124, 20, EDIT_FIELDS);
        this.editBox.setMaxLength(999999);
        this.editBox.setValue(valueToString(currentValue));
        this.editBox.setResponder(this::valueFromString);
        this.addChild(this.editBox);
    }

    @Override
    public void resetValue() {
        super.resetValue();
        this.editBox.setValue(valueToString(getValue()));
    }

    public void valueFromString(String value) {
        try {
            DataResult<T> result = parseFromString(value);
            Optional<DataResult.Error<T>> error = result.error();
            error.ifPresent(e -> bindError(e.toString()));
            if (error.isPresent()) return;
            setValue(result.getOrThrow()); // Error already handled, but if it wasn't, would be bound anyway
            unbindError();
        } catch (Throwable e) {
            bindError("Invalid input: " + e);
        }
    }

    public abstract DataResult<T> parseFromString(String value) ;

    public String valueToString(T value) {
        if (value == null) return "";
        return value.toString();
    }

    @Override
    public void extractContents(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        extractNameAndTooltip(graphics, hovered, 1, mouseX, mouseY);
        this.editBox.setEditable(isEditable());
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        this.editBox.setPosition(valueX, getPaddedY());
        this.editBox.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContents(graphics, mouseX, mouseY, hovered, a);
    }

    @Override
    public int getValueWidth() {
        return 124;
    }

    public void bindError(String error) {
        this.error = Component.literal("Invalid input: " + error);
    }

    public void unbindError() {
        this.error = null;
    }

    @Override
    public Optional<Component> error() {
        return Optional.ofNullable(this.error);
    }
}
