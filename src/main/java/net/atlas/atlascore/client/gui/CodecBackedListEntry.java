//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package net.atlas.atlascore.client.gui;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import me.shedaniel.clothconfig2.gui.entries.TextFieldListEntry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.ApiStatus.Internal;

@OnlyIn(Dist.CLIENT)
public class CodecBackedListEntry<T> extends TextFieldListEntry<Tag> {
    private final Codec<T> codec;
    /** @deprecated */
    @Deprecated
    @Internal
    public CodecBackedListEntry(Component fieldName, Codec<T> codec, Tag value, Component resetButtonKey, Supplier<Tag> defaultValue, Consumer<Tag> saveConsumer) {
        super(fieldName, value, resetButtonKey, defaultValue);
        this.saveCallback = saveConsumer;
        this.codec = codec;
    }

    /** @deprecated */
    @Deprecated
    @Internal
    public CodecBackedListEntry(Component fieldName, Codec<T> codec, Tag value, Component resetButtonKey, Supplier<Tag> defaultValue, Consumer<Tag> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier) {
        this(fieldName, codec, value, resetButtonKey, defaultValue, saveConsumer, tooltipSupplier, false);
    }

    /** @deprecated */
    @Deprecated
    @Internal
    public CodecBackedListEntry(Component fieldName, Codec<T> codec, Tag value, Component resetButtonKey, Supplier<Tag> defaultValue, Consumer<Tag> saveConsumer, Supplier<Optional<Component[]>> tooltipSupplier, boolean requiresRestart) {
        super(fieldName, value, resetButtonKey, defaultValue, tooltipSupplier, requiresRestart);
        this.saveCallback = saveConsumer;
        this.codec = codec;
    }

    @Override
    protected boolean isChanged(Tag original, String s) {
        return !original.getAsString().equals(s);
    }

    public Tag getValue() {
        StringReader reader = new StringReader(this.textFieldWidget.getValue());
        try {
            return new TagParser(reader).readValue();
        } catch (CommandSyntaxException e) {
            return new CompoundTag();
        }
    }

    @Override
    public Optional<Component> getError() {
        StringReader reader = new StringReader(this.textFieldWidget.getValue());
        AtomicReference<Optional<Component>> optional = new AtomicReference<>(Optional.empty());
        try {
            Tag tag = new TagParser(reader).readValue();
            DataResult<T> dataResult = codec.parse(NbtOps.INSTANCE, tag);
            dataResult.mapError(error -> {
                optional.set(Optional.of(Component.literal(error)));
                return error;
            });
        } catch (CommandSyntaxException e) {
            return Optional.of(Component.literal(e.toString()));
        }
        return optional.get();
    }
}