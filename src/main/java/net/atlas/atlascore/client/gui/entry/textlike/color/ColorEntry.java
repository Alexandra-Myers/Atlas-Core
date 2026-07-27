package net.atlas.atlascore.client.gui.entry.textlike.color;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.DataResult;
import net.atlas.atlascore.client.gui.entry.textlike.TextLikeEntry;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.atlas.atlascore.client.gui.entry.EnumEntry.snakeCaseToName;

public class ColorEntry extends TextLikeEntry<Integer> {
    private final ColorDisplayWidget colorDisplayWidget;
    private final boolean hasAlpha;

    public ColorEntry(Integer currentValue, Supplier<Integer> defaultValue, boolean hasAlpha, boolean restartRequired, @Nullable Component name, Supplier<Optional<Component[]>> tooltip, Consumer<Integer> saveCallback) {
        super(currentValue, defaultValue, restartRequired, name, tooltip, saveCallback);
        this.hasAlpha = hasAlpha;
        this.editBox.setValue(valueToString(currentValue)); // Kind of redundant but we want hasAlpha to be set by the time value is updated
        this.colorDisplayWidget = new ColorDisplayWidget(0, 0, 20, this.editBox::isFocused, () ->
                this.hasAlpha ? this.getValue() : 0xFF000000 | this.getValue());
        this.addChild(this.colorDisplayWidget);
    }

    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        int valueX = getX() + getWidth() - getValueWidth() - getResetWidth() - 5;
        int colorDisplayX = valueX + this.editBox.getWidth() + 2;
        this.colorDisplayWidget.setPosition(colorDisplayX, this.getPaddedY());

        this.colorDisplayWidget.extractRenderState(graphics, mouseX, mouseY, a);
        super.extractContent(graphics, mouseX, mouseY, hovered, a);
    }

    public Optional<Component> error() {
        return this.getResultSafe(this.editBox.getValue()).right();
    }

    @Override
    public DataResult<Integer> parseFromString(String value) throws NumberFormatException {
        Either<Integer, Component> output = this.getResult(value);
        return output.map(DataResult::success, error -> DataResult.error(() -> "Invalid Color: " + error.tryCollapseToString()));
    }

    @Override
    public String valueToString(Integer value) {
        if (value == null) return "";
        return AtlasConfig.ColorHolder.toColorHex(this.hasAlpha, value);
    }

    @Override
    public int getValueWidth() {
        return super.getValueWidth() + 22;
    }

    protected Either<Integer, Component> getResult(String str) throws NumberFormatException {
        int color;
        String stripped = str;
        if (str.startsWith("#")) {
            stripped = AtlasConfig.stripHexStarter(str);
            if (stripped.length() > 8) {
                return Either.right(error("hex_too_long", stripped, 8));
            }

            if (!this.hasAlpha && stripped.length() > 6) {
                return Either.right(error("hex_has_alpha", stripped));
            }

            color = (int)Long.parseLong(stripped, 16);
        } else {
            color = (int)Long.parseLong(str);
        }

        int a = color >> 24 & 255;
        if (!this.hasAlpha && a > 0) {
            return Either.right(error("hex_has_alpha", stripped));
        } else {
            return Either.left(color);
        }
    }

    protected Either<Integer, Component> getResultSafe(String str) {
        try {
            return this.getResult(str);
        } catch (NumberFormatException e) {
            return Either.right(Component.literal("Invalid input: " + e));
        }
    }

    public static Component error(@NonNull String error, Object... args) {
        return Component.translatableWithFallback("text.config.error.color." + error, snakeCaseToName(error), args);
    }
}
