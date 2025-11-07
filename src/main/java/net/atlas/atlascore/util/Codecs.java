package net.atlas.atlascore.util;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.atlas.atlascore.AtlasCore;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static net.atlas.atlascore.command.argument.Argument.Arguments.PRIMITIVE_TO_WRAPPER;
import static net.minecraft.util.ExtraCodecs.validate;

public class Codecs {
    public static Codec<Double> doubleRangeMinInclusiveWithMessage(double d, double e, Function<Double, String> function) {
        return validate(Codec.DOUBLE, (double_) -> double_.compareTo(d) >= 0 && double_.compareTo(e) <= 0 ? DataResult.success(double_) : DataResult.error(() -> function.apply(double_)));
    }

    public static Codec<Double> doubleRangeMinExclusiveWithMessage(double d, double e, Function<Double, String> function) {
        return validate(Codec.DOUBLE, (double_) -> double_.compareTo(d) > 0 && double_.compareTo(e) <= 0 ? DataResult.success(double_) : DataResult.error(() -> function.apply(double_)));
    }

    public static Codec<Double> doubleRange(double d, double e) {
        return doubleRangeMinInclusiveWithMessage(d, e, (double_) -> "Value must be within range [" + d + ";" + e + "]: " + double_);
    }
    public static <S, V> List<V> getArguments(CommandContext<S> context, Class<V> clazz) {
        return getArguments(context).values().stream()
                .map(ParsedArgument::getResult)
                .filter(argument -> PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(argument.getClass()))
                .map(argument -> (V) argument)
                .toList();
    }

    public static <S> boolean hasArgument(CommandContext<S> context, String name) {
        return getArguments(context).containsKey(name);
    }

    private static <S> Map<String, ParsedArgument<S, ?>> getArguments(CommandContext<S> context) {
        Map<String, ParsedArgument<S, ?>> argumentMap;
        try {
            Field arguments = CommandContext.class.getDeclaredField("arguments");
            arguments.setAccessible(true);
            argumentMap = (Map<String, ParsedArgument<S, ?>>) arguments.get(context);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            argumentMap = new HashMap<>();
            AtlasCore.LOGGER.error("Failed to get command arguments! Exception: {}", e);
        }
        return argumentMap;
    }
}