package net.atlas.atlascore.mixin;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import net.atlas.atlascore.extensions.CommandContextExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Map;

@Mixin(CommandContext.class)
public class CommandContextMixin<S> implements CommandContextExtensions {
    @Shadow @Final private Map<String, ParsedArgument<S, ?>> arguments;

    @Shadow @Final private static Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    @SuppressWarnings("unchecked")
    @Override
    public <V> List<V> getArguments(Class<V> clazz) {
        return arguments.values().stream()
                .map(ParsedArgument::getResult)
                .filter(argument -> PRIMITIVE_TO_WRAPPER.getOrDefault(clazz, clazz).isAssignableFrom(argument.getClass()))
                .map(argument -> (V) argument)
                .toList();
    }

    @Override
    public boolean hasArgument(String name) {
        return arguments.containsKey(name);
    }
}
