package net.atlas.atlascore.util;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public record JavaToJSONSerialisation<T>(BiFunction<AtlasConfig.ConfigHolder<T, RegistryFriendlyByteBuf>, JsonObject, T> decoder, BiConsumer<JsonWriter, T> encoder) {
}
