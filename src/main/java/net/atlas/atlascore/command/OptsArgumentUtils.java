package net.atlas.atlascore.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.command.argument.Argument;
import net.atlas.atlascore.command.argument.OptsArgument;
import net.atlas.atlascore.util.MapUtils;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class OptsArgumentUtils {
    public static final Function<SuggestionsBuilder, CompletableFuture<Suggestions>> SUGGEST_NOTHING = SuggestionsBuilder::buildFuture;
    /**
     * Adds opts arguments onto the end of the given command argument builder.
     *
     * @param openingArg The argument builder the appended arguments should come after
     * @param trueArguments The official names for these arguments inside Brigadier. These should be generic names as the order is unspecified.
     * @param command The command to execute when any of these arguments are sent in. For how to receive an instance of {@link Argument.Arguments}, refer to the method {@link Argument#argumentMap(OptsArgument, CommandContext, String)}
     * @param possibleArguments The actual opts arguments, mapped from the name to an instantiated argument type to be used to parse them.
     * @see Argument#argumentMap(OptsArgument, CommandContext, String)
     * @see MapUtils#buildHashMapFromAlignedArrays(Object[], Object[])
     * @return An array of both the original argument builder [0], and the new argument builder after adding the arguments.
     */
    @SuppressWarnings("unchecked")
    public static ArgumentBuilder<CommandSourceStack, ?>[] appendArguments(ArgumentBuilder<CommandSourceStack, ?> openingArg, String[] trueArguments, Function<OptsArgument, Command<CommandSourceStack>> command, Map<String, ArgumentType<?>> possibleArguments) {
        ArgumentBuilder<CommandSourceStack, ?> base = openingArg;
        OptsArgument optsArgument = OptsArgument.fromMap(possibleArguments);
        for (String arg : trueArguments) {
            ArgumentBuilder<CommandSourceStack, ?> next = Commands.argument(arg, ResourceLocationArgument.id())
                    .suggests(optsArgument::suggestions).executes(command.apply(optsArgument));
            base.then(next);
            base = next;
        }
        return new ArgumentBuilder[]{openingArg, base};
    }
}
