package net.atlas.atlascore.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ConfigHolderLike;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@SuppressWarnings("unchecked")
public record ConfigHolderArgument(String configArgument) implements ExtendedArgumentType<ConfigHolderLike<?, ? extends ByteBuf>> {
    public static final DynamicCommandExceptionType ERROR_MALFORMED_HOLDER = new DynamicCommandExceptionType(
            (object) -> Component.translatableEscape("arguments.config.holder.malformed", object)
    );
    public static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
    private static final Collection<String> EXAMPLES = List.of("grayFormattingColour");

    public static ConfigHolderArgument configHolderArgument(String parentConfigArgument) {
        return new ConfigHolderArgument(parentConfigArgument);
    }

    public static ConfigHolderLike<?, ? extends ByteBuf> getConfigHolder(final CommandContext<?> context, String name) {
        return context.getArgument(name, ConfigHolderLike.class);
    }

    public ConfigHolderLike<?, ? extends ByteBuf> parse(StringReader stringReader) throws CommandSyntaxException {
        // Literally nothing we can do to change this fate
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    }

    public static String readHolderName(StringReader stringReader) {
        int i = stringReader.getCursor();

        while (stringReader.canRead() && stringReader.peek() != '[' && stringReader.peek() != ']' && !Character.isWhitespace(stringReader.peek())) {
            stringReader.skip();
        }

        return stringReader.getString().substring(i, stringReader.getCursor());
    }

    @Override
    public <S> ConfigHolderLike<?, ? extends ByteBuf> parse(StringReader reader, CommandContext<S> commandContext) throws CommandSyntaxException {
        AtlasConfig.ConfigHolder<?, ? extends ByteBuf> configHolder = AtlasConfigArgument.getConfig(commandContext, configArgument).valueNameToConfigHolderMap.get(readHolderName(reader));
        ConfigHolderLike<?, ? extends ByteBuf> inner = null;
        ConfigHolderLike<?, ? extends ByteBuf> baseHolder = configHolder;
        int unresolvedInners = 0;
        boolean isExtended = configHolder instanceof AtlasConfig.ExtendedHolder;
        while (isExtended && reader.canRead() && reader.peek() == '[') {
            reader.expect('[');
            inner = ((AtlasConfig.ExtendedHolder)baseHolder).findInner(reader);
            baseHolder = inner;
            isExtended = baseHolder instanceof AtlasConfig.ExtendedHolder;
            unresolvedInners++;
            if (!isExtended) {
                while (unresolvedInners > 0 && reader.canRead()) {
                    reader.expect(']');
                    unresolvedInners--;
                }
            }
        }
        return inner == null ? configHolder : inner;
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        SuggestionsVisitor visitor = new SuggestionsVisitor();
        visitor.visitSuggestions((suggestionsBuilder) -> SharedSuggestionProvider.suggest(AtlasConfigArgument.getConfig(commandContext, configArgument).valueNameToConfigHolderMap.keySet(), builder));
        ConfigHolderLike<?, ? extends ByteBuf> configHolderLike;
        int cursor = reader.getCursor();
        String currentHolderName = readHolderName(reader);
        boolean isExtended;
        Map<String, AtlasConfig.ConfigHolder<?, ? extends ByteBuf>> valueNameToConfigHolderMap = AtlasConfigArgument.getConfig(commandContext, configArgument).valueNameToConfigHolderMap;
        if (valueNameToConfigHolderMap.containsKey(currentHolderName)) {
            configHolderLike = valueNameToConfigHolderMap.get(currentHolderName);
            isExtended = configHolderLike instanceof AtlasConfig.ExtendedHolder;
        } else {
            reader.setCursor(cursor);
            return visitor.resolveSuggestions(builder, reader);
        }
        if (!isExtended) {
            reader.setCursor(cursor);
            return visitor.resolveSuggestions(builder, reader);
        }
        visitor.visitSuggestions(this::suggestStartInner);
        int unresolvedInners = 0;
        while (isExtended && reader.canRead() && reader.peek() == '[') {
            visitor.visitSuggestions(SUGGEST_NOTHING);
            AtlasConfig.ExtendedHolder extendedHolder = (AtlasConfig.ExtendedHolder) configHolderLike;
            try {
                reader.expect('[');
                visitor.visitSuggestions((suggestionsBuilder) -> extendedHolder.suggestInner(reader, suggestionsBuilder));
                cursor = reader.getCursor();
                currentHolderName = readHolderName(reader);
                ConfigHolderLike<?, ? extends ByteBuf> temp = extendedHolder.retrieveInner(currentHolderName);
                if (temp instanceof AtlasConfig.ExtendedHolder) {
                    configHolderLike = temp;
                    visitor.visitSuggestions(this::suggestStartInceptionOrEnd);
                    unresolvedInners++;
                } else {
                    if (temp != null) {
                        visitor.visitSuggestions(SUGGEST_NOTHING);
                        isExtended = false;
                        unresolvedInners++;
                    } else reader.setCursor(cursor);
                }
                if (!isExtended) {
                    do {
                        if (!reader.canRead() || reader.peek() != ']') visitor.visitSuggestions(this::suggestEnd);
                        if (reader.canRead()) reader.expect(']');
                        unresolvedInners--;
                    } while (unresolvedInners > 0);
                }
            } catch (CommandSyntaxException ignored) {

            }
        }
        return visitor.resolveSuggestions(builder, reader);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private CompletableFuture<Suggestions> suggestStartInner(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf('['));
        }

        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestStartInceptionOrEnd(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf('['));
            suggestionsBuilder.suggest(String.valueOf(']'));
        }

        return suggestionsBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestEnd(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf(']'));
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

    public static class HolderInfo implements ArgumentTypeInfo<ConfigHolderArgument, HolderInfo.Template> {
        public void serializeToNetwork(HolderInfo.Template template, FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeUtf(template.configArgument);
        }

        public void serializeToJson(HolderInfo.Template template, JsonObject jsonObject) {
            jsonObject.addProperty("configArgument", template.configArgument);
        }

        public HolderInfo.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            return new HolderInfo.Template(friendlyByteBuf.readUtf());
        }

        public HolderInfo.Template unpack(ConfigHolderArgument argumentType) {
            return new HolderInfo.Template(argumentType.configArgument);
        }

        public final class Template implements ArgumentTypeInfo.Template<ConfigHolderArgument> {
            private final String configArgument;

            public Template(final String configArgument) {
                this.configArgument = configArgument;
            }

            public ConfigHolderArgument instantiate(CommandBuildContext commandBuildContext) {
                return configHolderArgument(configArgument);
            }

            public ArgumentTypeInfo<ConfigHolderArgument, ?> type() {
                return HolderInfo.this;
            }
        }
    }

    public record ConfigValueArgument(String holderArgument) implements ExtendedArgumentType<Object> {
        private static final Collection<String> EXAMPLES = Arrays.asList("1", "2.03", "foo", "string", "true", "#FFFFFF");

        public static ConfigValueArgument configValueArgument(String parentHolderArgument) {
            return new ConfigValueArgument(parentHolderArgument);
        }

        public Object parse(StringReader stringReader) throws CommandSyntaxException {
            // Literally nothing we can do to change this fate
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }

        public <S> Object parse(final StringReader reader, S source, final CommandContext<S> commandContext) throws CommandSyntaxException {
            return ConfigHolderArgument.getConfigHolder(commandContext, holderArgument).parse(reader, source, commandContext);
        }

        @Override
        public <S> Object parse(StringReader reader, CommandContext<S> commandContext) throws CommandSyntaxException {
            return parse(reader, commandContext.getSource(), commandContext);
        }

        public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
            return ConfigHolderArgument.getConfigHolder(commandContext, holderArgument).buildSuggestions(commandContext, suggestionsBuilder);
        }

        public Collection<String> getExamples() {
            return EXAMPLES;
        }
        public static class ValueInfo implements ArgumentTypeInfo<ConfigValueArgument, ValueInfo.Template> {
            public void serializeToNetwork(ValueInfo.Template template, FriendlyByteBuf friendlyByteBuf) {
                friendlyByteBuf.writeUtf(template.holderArgument);
            }

            public void serializeToJson(ValueInfo.Template template, JsonObject jsonObject) {
                jsonObject.addProperty("holderArgument", template.holderArgument);
            }

            public ValueInfo.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
                return new ValueInfo.Template(friendlyByteBuf.readUtf());
            }

            public ValueInfo.Template unpack(ConfigValueArgument argumentType) {
                return new ValueInfo.Template(argumentType.holderArgument);
            }

            public final class Template implements ArgumentTypeInfo.Template<ConfigValueArgument> {
                private final String holderArgument;

                public Template(final String holderArgument) {
                    this.holderArgument = holderArgument;
                }

                public ConfigValueArgument instantiate(CommandBuildContext commandBuildContext) {
                    return configValueArgument(holderArgument);
                }

                public ArgumentTypeInfo<ConfigValueArgument, ?> type() {
                    return ValueInfo.this;
                }
            }
        }
    }
}
