//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.atlas.atlascore.client.gui;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus.Internal;

@Environment(EnvType.CLIENT)
public class StringListEntry extends TextFieldListEntry<String> {
    /** @deprecated */
    @Deprecated
    @Internal
    public StringListEntry(Component fieldName, String value, Component resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer) {
        super(fieldName, value, resetButtonKey, defaultValue);
        this.saveCallback = saveConsumer;
    }

    /** @deprecated */
    @Deprecated
    @Internal
    public StringListEntry(Component fieldName, String value, Component resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier) {
        this(fieldName, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
    }

    /** @deprecated */
    @Deprecated
    @Internal
    public StringListEntry(Component fieldName, String value, Component resetButtonKey, Supplier<String> defaultValue, Consumer<String> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
        super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.saveCallback = saveConsumer;
    }

    public String getValue() {
        return this.textFieldWidget.getValue();
    }
}