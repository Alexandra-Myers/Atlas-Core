package net.atlas.atlascore.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.command.argument.ExtendedArgumentType;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

public class FieldHolder implements ConfigHolderLike<Object> {
    public final AtlasConfig.ConfigHolder<?> owner;
    private final Field field;
    private final ArgumentType<?> type;
    private final Object defaultValue;
    private final String name;
    public Object parsedValue;

    public FieldHolder(AtlasConfig.ConfigHolder<?> owner,
                       Field field,
                       ArgumentType<?> type,
                       Object defaultValue,
                       String name) {
        this.owner = owner;
        this.field = field;
        this.type = type;
        this.defaultValue = defaultValue;
        this.name = name;
    }
    @Override
    public void setToParsedValue() {
        if (parsedValue == null)
            return;
        setValue(parsedValue);
        parsedValue = null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public AtlasConfig.ConfigHolder<Object> getAsHolder() {
        return (AtlasConfig.ConfigHolder<Object>) owner;
    }

    @Override
    public Object get() {
        try {
            return field.get(owner.get());
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setValue(Object newValue) {
        try {
            var o = owner.get();
            field.set(o, newValue);
            owner.setValueAmbiguousType(o);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void resetValue() {
        setValue(defaultValue);
    }

    @Override
    public <S> Object parse(StringReader reader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
        Object ret;
        if (type instanceof ExtendedArgumentType<?> extendedArgumentType) ret = extendedArgumentType.parse(reader, commandContext);
        else ret = type.parse(reader);
        parsedValue = ret;
        return ret;
    }

    @Override
    public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return type.listSuggestions(commandContext, suggestionsBuilder);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean hasParsedValue() {
        return parsedValue != null;
    }
}
