package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.util.Codecs;
import net.atlas.atlascore.util.MapUtils;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static net.atlas.atlascore.command.OptsArgumentUtils.SUGGEST_NOTHING;

public record OptsArgument(Map<String, ArgumentType<?>> arguments) {
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
    public @Nullable Argument<?> getArgument(final CommandContext<?> context, String name) throws CommandSyntaxException {
        StringReader stringReader = new StringReader(context.getArgument(name, ResourceLocation.class).toString());
        String argumentName = readArgumentName(stringReader);
        List<Argument<?>> args = readArguments(context);
        return args.stream().filter(argument -> argument.name().equals(argumentName)).findFirst().orElse(null);
    }
    /**
     * Parses and obtains all {@link Argument}s from the {@link CommandContext}.
     *
     * @param context The context for the command, as provided by Brigadier.
     * @return Parsed {@link Argument}s from the {@link CommandContext}.
     */
    public List<Argument<?>> readArguments(final CommandContext<?> context) throws CommandSyntaxException {
        List<Argument<?>> args = new ArrayList<>();
        for (String realArgument : Codecs.getArguments(context, String.class)) {
            StringReader stringReader = new StringReader(context.getArgument(realArgument, String.class));
            String argumentName = readArgumentName(stringReader);
            if (!arguments.containsKey(argumentName)) continue; // Must not be an argument, then...
            stringReader.expect(':');
            ArgumentType<?> type = arguments.get(argumentName);
            Argument<?> ret = buildArgument(stringReader, context, type, argumentName);
            args.add(ret);
        }
        return args;
    }
    
    private static String readArgumentName(StringReader stringReader) {
        int i = stringReader.getCursor();

        while (stringReader.canRead() && stringReader.peek() != ':' && !Character.isWhitespace(stringReader.peek())) {
            stringReader.skip();
        }

        return stringReader.getString().substring(i, stringReader.getCursor());
    }

    @SuppressWarnings("unchecked")
    private <S, T> Argument<T> buildArgument(StringReader stringReader, CommandContext<S> commandContext, ArgumentType<T> type, String argumentName) throws CommandSyntaxException {
        T result = type.parse(stringReader, commandContext.getSource());
        return new Argument<>(argumentName, result, (Class<T>) result.getClass());
    }

    /**
     * Lists suggestions based on the {@link CommandContext}
     *
     * @param context The context for the command, as provided by Brigadier.
     * @param builder The suggestions builder to be used for suggesting options.
     * @return A {@link CompletableFuture} for the suggestions provided by the command.
     */
    public <S> CompletableFuture<Suggestions> suggestions(CommandContext<S> context, SuggestionsBuilder builder) throws CommandSyntaxException {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());
        SuggestionsVisitor visitor = new SuggestionsVisitor();
        List<Argument<?>> args = readArguments(context);
        List<String> argumentNames = arguments.keySet().stream().filter(string -> {
            AtomicBoolean ret = new AtomicBoolean(true);
            args.forEach(arg -> ret.set(ret.get() & !arg.name().equals(string)));
            return ret.get();
        }).toList();
        visitor.visitSuggestions(suggestionsBuilder -> SharedSuggestionProvider.suggest(argumentNames, builder));
        try {
            suggestArgument(visitor, context, reader, argumentNames);
        } catch (CommandSyntaxException ignored) {

        }
        return visitor.resolveSuggestions(builder, reader);
    }

    private <S> void suggestArgument(SuggestionsVisitor visitor, CommandContext<S> context, StringReader reader, List<String> argumentNames) throws CommandSyntaxException {
        int cursor = reader.getCursor();
        String argumentName = readArgumentName(reader);
        if (!argumentNames.contains(argumentName)) {
            reader.setCursor(cursor);
            throw ERROR_INVALID_ARGUMENT.createWithContext(reader, argumentName);
        }
        visitor.visitSuggestions(this::suggestSetValue);
        reader.expect(':');
        visitor.visitSuggestions(builder -> arguments.get(argumentName).listSuggestions(context, builder));
    }

    private CompletableFuture<Suggestions> suggestSetValue(SuggestionsBuilder suggestionsBuilder) {
        if (suggestionsBuilder.getRemaining().isEmpty()) {
            suggestionsBuilder.suggest(String.valueOf(':'));
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
}
