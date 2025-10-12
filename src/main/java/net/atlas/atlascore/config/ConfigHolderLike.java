package net.atlas.atlascore.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public interface ConfigHolderLike<T> {
    void setToParsedValue();

    AtlasConfig.ConfigHolder<T> getAsHolder();

    T get();

    void setValue(T newValue);

    void resetValue();

    <S> T parse(StringReader reader, S source, CommandContext<S> commandContext) throws CommandSyntaxException;

    <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder);

    <S> void verifySuggestionsArePresent(CommandContext<S> commandContext, StringReader reader) throws CommandSyntaxException;

    String getName();

    boolean hasParsedValue();
}
