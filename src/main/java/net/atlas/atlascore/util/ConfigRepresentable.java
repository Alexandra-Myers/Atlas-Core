package net.atlas.atlascore.util;

import com.mojang.brigadier.arguments.ArgumentType;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.atlas.atlascore.config.AtlasConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

public interface ConfigRepresentable<T> {
    void setOwnerHolder(AtlasConfig.ConfigHolder<T, RegistryFriendlyByteBuf> configHolder);
    List<String> fields();

    Component getFieldValue(String name);

    Component getFieldName(String name);

    void listField(String name, Consumer<Component> input);
    void listFields(Consumer<Component> input);
    Field fieldRepresentingHolder(String name);
    ArgumentType<?> argumentTypeRepresentingHolder(String name);
    @Environment(EnvType.CLIENT)
    List<AbstractConfigListEntry<?>> transformIntoConfigEntries();
}
