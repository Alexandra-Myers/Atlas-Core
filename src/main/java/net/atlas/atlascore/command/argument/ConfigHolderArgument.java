package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ConfigHolderLike;
import net.atlas.atlascore.config.ExtendedHolder;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static net.atlas.atlascore.command.OptsArgumentUtils.SUGGEST_NOTHING;

public record ConfigHolderArgument(String configArgument) {
    public static final DynamicCommandExceptionType ERROR_MALFORMED_HOLDER = new DynamicCommandExceptionType(
            (object) -> Component.translatable("arguments.config.holder.malformed", object)
    );
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_HOLDER = new DynamicCommandExceptionType(
            (object) -> Component.translatable("arguments.config.holder.unknown", object)
    );

    public static StringArgumentType configHolderArgument() {
        return StringArgumentType.word();
    }

    public static ConfigHolderLike<?> getConfigHolder(final CommandContext<?> context, String name, String configArgument) throws CommandSyntaxException {
        StringReader reader = new StringReader(context.getArgument(name, String.class));
        int cursor = reader.getCursor();
        String configHolderName = readHolderName(reader);
        AtlasConfig.ConfigHolder<?> configHolder = AtlasConfigArgument.getConfig(context, configArgument, false).valueNameToConfigHolderMap.get(configHolderName);
        if (configHolder == null) {
            reader.setCursor(cursor);
            throw ERROR_UNKNOWN_HOLDER.createWithContext(reader, configHolderName);
        }
        ConfigHolderLike<?> inner = null;
        ConfigHolderLike<?> baseHolder = configHolder;
        try {
            boolean isExtended = configHolder instanceof ExtendedHolder;
            while (isExtended && reader.canRead() && reader.peek() == '.') {
                reader.skip();
                inner = ((ExtendedHolder) baseHolder).findInner(reader);
                baseHolder = inner;
                isExtended = baseHolder instanceof ExtendedHolder;
            }
        } catch (CommandSyntaxException e) {
            reader.setCursor(cursor);
            throw e;
        }
        return inner == null ? configHolder : inner;
    }

    public static String readHolderName(StringReader stringReader) {
        int i = stringReader.getCursor();

        while (stringReader.canRead() && stringReader.peek() != '=' && stringReader.peek() != '.' && !Character.isWhitespace(stringReader.peek())) {
            stringReader.skip();
        }

        return stringReader.getString().substring(i, stringReader.getCursor());
    }

    public static <S> CompletableFuture<Suggestions> suggestions(CommandContext<S> commandContext, SuggestionsBuilder builder, String configArgument) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        SuggestionsVisitor visitor = new SuggestionsVisitor();
        visitor.visitSuggestions((suggestionsBuilder) -> SharedSuggestionProvider.suggest(AtlasConfigArgument.getConfig(commandContext, configArgument, false).valueNameToConfigHolderMap.keySet(), builder));
        try {
            parseHolder(visitor, reader, AtlasConfigArgument.getConfig(commandContext, configArgument, false));
        } catch (CommandSyntaxException ignored) {

        }
        return visitor.resolveSuggestions(builder, reader);
    }

    private static void parseHolder(SuggestionsVisitor visitor, StringReader reader, AtlasConfig atlasConfig) throws CommandSyntaxException {
        ConfigHolderLike<?> configHolderLike;
        int cursor = reader.getCursor();
        String currentHolderName = readHolderName(reader);
        boolean isExtended;
        Map<String, AtlasConfig.ConfigHolder<?>> valueNameToConfigHolderMap = atlasConfig.valueNameToConfigHolderMap;
        if (!valueNameToConfigHolderMap.containsKey(currentHolderName)) {
            reader.setCursor(cursor);
            throw ERROR_UNKNOWN_HOLDER.createWithContext(reader, currentHolderName);
        }
        configHolderLike = valueNameToConfigHolderMap.get(currentHolderName);
        isExtended = configHolderLike instanceof ExtendedHolder;
        if (isExtended) {
            visitor.visitSuggestions(ConfigHolderArgument::suggestStartInner);
            while (isExtended) {
                reader.expect('.');
                ExtendedHolder extendedHolder = (ExtendedHolder) configHolderLike;
                visitor.visitSuggestions((suggestionsBuilder) -> extendedHolder.suggestInner(reader, suggestionsBuilder));
                cursor = reader.getCursor();
                currentHolderName = readHolderName(reader);
                ConfigHolderLike<?> temp = extendedHolder.retrieveInner(currentHolderName);
                if (temp instanceof ExtendedHolder) {
                    configHolderLike = temp;
                    visitor.visitSuggestions(ConfigHolderArgument::suggestStartInner);
                } else if (temp == null) {
                    reader.setCursor(cursor);
                    throw ERROR_UNKNOWN_HOLDER.createWithContext(reader, currentHolderName);
                } else {
                    visitor.visitSuggestions(SUGGEST_NOTHING);
                    isExtended = false;
                }
            }
        }
    }

    private static CompletableFuture<Suggestions> suggestStartInner(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf('.'));
        }

        return suggestionsBuilder.buildFuture();
    }

    static class SuggestionsVisitor {
        private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

        public void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> function) {
            this.suggestions = function;
        }

        public CompletableFuture<Suggestions> resolveSuggestions(SuggestionsBuilder suggestionsBuilder, StringReader stringReader) {
            return this.suggestions.apply(suggestionsBuilder.createOffset(stringReader.getCursor()));
        }
    }

    public record ConfigValueArgument() {
        public static <S> void readArgument(CommandContext<S> commandContext, String name, ConfigHolderLike<?> holder) throws CommandSyntaxException {
            StringReader reader = new StringReader(commandContext.getArgument(name, String.class));
            holder.parse(reader, commandContext.getSource(), commandContext);
        }

        public static <S> CompletableFuture<Suggestions> suggestions(CommandContext<S> commandContext, SuggestionsBuilder builder, String holderArgument, String configArgument) throws CommandSyntaxException {
            StringReader reader = new StringReader(builder.getInput());
            SuggestionsVisitor suggestionsVisitor = new SuggestionsVisitor();
            reader.setCursor(builder.getStart());
            try {
                int cursor = reader.getCursor();
                ConfigHolderLike<?> configHolder = ConfigHolderArgument.getConfigHolder(commandContext, holderArgument, configArgument);
                suggestionsVisitor.visitSuggestions(builder1 -> configHolder.buildSuggestions(commandContext, builder1.createOffset(cursor)));
                configHolder.verifySuggestionsArePresent(commandContext, reader);
            } catch (CommandSyntaxException ignored) {

            }
            return suggestionsVisitor.resolveSuggestions(builder, reader);
        }
    }
}