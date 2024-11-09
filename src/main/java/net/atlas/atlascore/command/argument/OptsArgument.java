package net.atlas.atlascore.command.argument;

import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.extensions.CommandContextExtensions;
import net.atlas.atlascore.util.MapUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.atlas.atlascore.command.OptsArgumentUtils.SUGGEST_NOTHING;

public record OptsArgument(Map<String, ArgumentType<?>> arguments) implements ExtendedArgumentType<Argument<?>> {
    public static final DynamicCommandExceptionType ERROR_INVALID_ARGUMENT = new DynamicCommandExceptionType(
            (object) -> Component.translatableEscape("arguments.chosen.argument.invalid", object)
    );
    
    public OptsArgument(String[] names, ArgumentType<?>[] types) {
        this(MapUtils.buildHashMapFromAlignedArrays(names, types));
    }

    /**
     * Creates an opts argument from two aligned arrays of names and argument types.
     *
     * @param names An array where names[0] will be mapped to types[0]. Any null names will be discarded.
     * @param types An array where types[0] can be obtained with names[0]. Any null types will be discarded.
     * @return A new {@link OptsArgument} for the given data.
     * @see MapUtils#buildHashMapFromAlignedArrays(Object[], Object[])
     * @see OptsArgument#fromSortedArray(Object...)
     * @see OptsArgument#fromMap(Map)
     */
    public static OptsArgument namesAndArgumentTypes(String[] names, ArgumentType<?>[] types) {
        return new OptsArgument(names, types);
    }

    /**
     * Creates an opts argument from a sorted array of names and argument types.
     *
     * @param namesAndTypes An array, such that an even number is of the type {@link String} and the following odd number is the {@link ArgumentType} it maps to.
     * @return A new {@link OptsArgument} for the given data.
     * @see OptsArgument#namesAndArgumentTypes(String[], ArgumentType[])
     * @see OptsArgument#fromMap(Map)
     */
    public static OptsArgument fromSortedArray(Object... namesAndTypes) {
        if (namesAndTypes.length % 2 == 1)
            throw new IllegalStateException("Arguments must have both a name and a type!");
        String[] names = new String[namesAndTypes.length / 2];
        ArgumentType<?>[] types = new ArgumentType[namesAndTypes.length / 2];
        for (int index = 0; index < namesAndTypes.length; index += 2) {
            int finalIndex = index / 2;
            if (namesAndTypes[index + 1] == null) continue;
            names[finalIndex] = (String) namesAndTypes[index];
            types[finalIndex] = (ArgumentType<?>) namesAndTypes[index + 1];
        }
        return new OptsArgument(names, types);
    }

    /**
     * Constructs an opts argument given a {@link Map} of names to argument types.
     *
     * @param arguments The map to provide to the constructor. null argument types will be discarded.
     * @return A new {@link OptsArgument} for the given data.
     * @see OptsArgument#namesAndArgumentTypes(String[], ArgumentType[])
     * @see OptsArgument#fromSortedArray(Object...)
     */
    public static OptsArgument fromMap(Map<String, ArgumentType<?>> arguments) {
        List<String> toRemove = new ArrayList<>();
        arguments.forEach((string, type) -> {
            if (type == null) toRemove.add(string);
        });
        toRemove.forEach(arguments::remove);
        return new OptsArgument(arguments);
    }

    /**
     * Obtains an {@link Argument} from the {@link CommandContext}.
     *
     * @param context The context for the command, as provided by Brigadier.
     * @param name The name for the Brigadier argument. This is separate to the resulting argument's name.
     * @return The previously parsed {@link Argument} inside of the command context.
     */
    public static Argument<?> getArgument(final CommandContext<?> context, String name) {
        return context.getArgument(name, Argument.class);
    }
    
    private static String readArgumentName(StringReader stringReader) {
        int i = stringReader.getCursor();

        while (stringReader.canRead() && stringReader.peek() != '=' && !Character.isWhitespace(stringReader.peek())) {
            stringReader.skip();
        }

        return stringReader.getString().substring(i, stringReader.getCursor());
    }

    @Override
    public <S> Argument<?> parse(StringReader stringReader, CommandContext<S> commandContext) throws CommandSyntaxException {
        int cursor = stringReader.getCursor();
        String argumentName = readArgumentName(stringReader);
        Stream<String> argumentNames = arguments.keySet().stream().filter(string -> {
            AtomicBoolean ret = new AtomicBoolean(true);
            ((CommandContextExtensions) commandContext).getArguments(Argument.class).forEach(arg -> ret.set(ret.get() & !arg.name().equals(string)));
            return ret.get();
        });
        if (!argumentNames.toList().contains(argumentName)) {
            stringReader.setCursor(cursor);
            throw ERROR_INVALID_ARGUMENT.createWithContext(stringReader, argumentName);
        }
        stringReader.expect('=');
        ArgumentType<?> type = arguments.get(argumentName);
        return buildArgument(stringReader, commandContext, type, argumentName);
    }

    @SuppressWarnings("unchecked")
    private <S, T> Argument<T> buildArgument(StringReader stringReader, CommandContext<S> commandContext, ArgumentType<T> type, String argumentName) throws CommandSyntaxException {
        T result = type instanceof ExtendedArgumentType<T> extendedArgumentType ? extendedArgumentType.parse(stringReader, commandContext.getSource(), commandContext) : type.parse(stringReader, commandContext.getSource());
        return new Argument<>(argumentName, result, (Class<T>) result.getClass());
    }

    @Override
    public Argument<?> parse(StringReader reader) throws CommandSyntaxException {
        // Literally nothing we can do to change this fate
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        SuggestionsVisitor visitor = new SuggestionsVisitor();
        Stream<String> argumentNames = arguments.keySet().stream().filter(string -> {
            AtomicBoolean ret = new AtomicBoolean(true);
            ((CommandContextExtensions) context).getArguments(Argument.class).forEach(arg -> ret.set(ret.get() & !arg.name().equals(string)));
            return ret.get();
        });
        visitor.visitSuggestions(suggestionsBuilder -> SharedSuggestionProvider.suggest(argumentNames, builder));
        try {
            suggestArgument(visitor, context, reader, argumentNames);
        } catch (CommandSyntaxException ignored) {

        }
        return visitor.resolveSuggestions(builder, reader);
    }

    private <S> void suggestArgument(SuggestionsVisitor visitor, CommandContext<S> context, StringReader reader, Stream<String> argumentNames) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String argumentName = readArgumentName(reader);
        if (!argumentNames.toList().contains(argumentName)) {
            reader.setCursor(cursor);
            throw ERROR_INVALID_ARGUMENT.createWithContext(reader, argumentName);
        }
        visitor.visitSuggestions(this::suggestSetValue);
        reader.expect('=');
        visitor.visitSuggestions(builder -> arguments.get(argumentName).listSuggestions(context, builder));
    }

    private CompletableFuture<Suggestions> suggestSetValue(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf('='));
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

    public static class Info implements ArgumentTypeInfo<OptsArgument, Info.Template> {
        @SuppressWarnings("unchecked")
        public void serializeToNetwork(Info.Template template, FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeMap(template.argumentInfos, FriendlyByteBuf::writeUtf, (byteBuf, argument) -> {
                byteBuf.writeVarInt(BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getId(argument.type()));
                ((ArgumentTypeInfo<?, ArgumentTypeInfo.Template<?>>) argument.type()).serializeToNetwork(argument, byteBuf);
            });
        }

        @SuppressWarnings("unchecked")
        public void serializeToJson(Info.Template template, JsonObject jsonObject) {
            JsonArray entries = new JsonArray();
            template.argumentInfos.forEach((argument, argTemplate) -> {
                if (argTemplate == null) return;
                JsonObject entry = new JsonObject();
                entry.addProperty("name", argument);
                JsonObject jsonObject1 = new JsonObject();
                jsonObject1.addProperty("parser", BuiltInRegistries.COMMAND_ARGUMENT_TYPE.getKey(argTemplate.type()).toString());
                JsonObject jsonObject2 = new JsonObject();
                ((ArgumentTypeInfo<?, ArgumentTypeInfo.Template<?>>) argTemplate.type()).serializeToJson(argTemplate, jsonObject2);
                if (!jsonObject2.isEmpty()) jsonObject1.add("properties", jsonObject2);
                entry.add("argument_template", jsonObject1);
                entries.add(entry);
            });
            jsonObject.add("configArgument", entries);
        }

        public Info.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            return new Info.Template(friendlyByteBuf);
        }

        public Info.Template unpack(OptsArgument argumentType) {
            return new Info.Template(argumentType.arguments);
        }

        public final class Template implements ArgumentTypeInfo.Template<OptsArgument> {
            private final Map<String, ArgumentTypeInfo.Template<?>> argumentInfos;

            public Template(final Map<String, ArgumentType<?>> arguments) {
                this.argumentInfos = Maps.transformValues(arguments, ArgumentTypeInfos::unpack);
            }

            public Template(FriendlyByteBuf friendlyByteBuf) {
                this.argumentInfos = friendlyByteBuf.readMap(FriendlyByteBuf::readUtf, byteBuf -> {
                    ArgumentTypeInfo<?, ?> argumentTypeInfo = BuiltInRegistries.COMMAND_ARGUMENT_TYPE.byId(byteBuf.readVarInt());

                    if (argumentTypeInfo != null) {
                        return argumentTypeInfo.deserializeFromNetwork(byteBuf);
                    } else return null;
                });
            }

            public OptsArgument instantiate(CommandBuildContext commandBuildContext) {
                return fromMap(Maps.transformValues(argumentInfos, info -> {
                    if (info == null) return null;
                    return info.instantiate(commandBuildContext);
                }));
            }

            public ArgumentTypeInfo<OptsArgument, ?> type() {
                return Info.this;
            }
        }
    }
}
