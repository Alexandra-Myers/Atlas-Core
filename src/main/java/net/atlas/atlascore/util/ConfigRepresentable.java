package net.atlas.atlascore.util;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.serialization.Codec;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.atlas.atlascore.config.AtlasConfig;
//? fabric {
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

@Deprecated(since = "1.2.0")
public interface ConfigRepresentable<T> {
    Codec<T> getCodec(AtlasConfig.ConfigHolder<T> owner);
    void setOwnerHolder(AtlasConfig.ConfigHolder<T> configHolder);
    List<String> fields();
    Component getFieldValue(String name);
    Component getFieldName(String name);
    void listField(String name, Consumer<Component> input);
    void listFields(Consumer<Component> input);
    Field fieldRepresentingHolder(String name);
    ArgumentType<?> argumentTypeRepresentingHolder(String name);
    @Deprecated(forRemoval = true, since = "1.2.0")
    //? fabric {
    @Environment(EnvType.CLIENT)
    //?}
    List<AbstractConfigListEntry<?>> transformIntoConfigEntries();
}
