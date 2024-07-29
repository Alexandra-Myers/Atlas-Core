package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unchecked")
public class ConfigHolderArgument implements ArgumentType<AtlasConfig.ConfigHolder<?, ? extends ByteBuf>>, ExtendedArgumentType<AtlasConfig.ConfigHolder<?, ? extends ByteBuf>> {
    private static final Collection<String> EXAMPLES = List.of("grayFormattingColour");

    public static ConfigHolderArgument configHolderArgument() {
        return new ConfigHolderArgument();
    }

    public static AtlasConfig.ConfigHolder<?, ? extends ByteBuf> getConfigHolder(final CommandContext<?> context, String name) {
        return context.getArgument(name, AtlasConfig.ConfigHolder.class);
    }

    public AtlasConfig.ConfigHolder<?, ? extends ByteBuf> parse(StringReader stringReader) throws CommandSyntaxException {
        // Literally nothing we can do to change this fate
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    }

    @Override
    public <S> AtlasConfig.ConfigHolder<?, ? extends ByteBuf> parse(StringReader reader, CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
        if (!contextBuilder.getArguments().containsKey("config")) {
            // Literally nothing we can do to change this fate
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        return ((AtlasConfig) contextBuilder.getArguments().get("config").getResult()).valueNameToConfigHolderMap.get(reader.readString());
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return SharedSuggestionProvider.suggest(AtlasConfigArgument.getConfig(commandContext, "config").valueNameToConfigHolderMap.keySet(), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class ConfigValueArgument implements ArgumentType<Object>, ExtendedArgumentType<Object> {
        private static final Collection<String> EXAMPLES = Arrays.asList("1", "2.03", "foo", "string", "true", "#FFFFFF");

        public static ConfigValueArgument configValueArgument() {
            return new ConfigValueArgument();
        }

        public Object parse(StringReader stringReader) throws CommandSyntaxException {
            // Literally nothing we can do to change this fate
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }

        public <S> Object parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException {
            if (!contextBuilder.getArguments().containsKey("holder")) {
                // Literally nothing we can do to change this fate
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
            }
            return ((AtlasConfig.ConfigHolder<?, ? extends ByteBuf>) contextBuilder.getArguments().get("holder").getResult()).parse(reader);
        }

        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
            return ConfigHolderArgument.getConfigHolder(commandContext, "holder").buildSuggestions(commandContext, suggestionsBuilder);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
    }
}
