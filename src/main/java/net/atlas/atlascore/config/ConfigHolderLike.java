package net.atlas.atlascore.config;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;

import java.util.concurrent.CompletableFuture;

public interface ConfigHolderLike<T, B extends ByteBuf> {
    void setToParsedValue();

    AtlasConfig.ConfigHolder<T, B> getAsHolder();

    T get();

    void setValue(T newValue);

    void resetValue();

    <S> T parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException;

    <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder);

    String getName();
}
