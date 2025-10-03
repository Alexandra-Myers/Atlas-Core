package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface ExtendedArgumentType<T> extends ArgumentType<T> {
    default <S> T parse(final StringReader reader, final S source, final CommandContext<S> commandContext) throws CommandSyntaxException {
        return parse(reader, commandContext);
    }
    default <S> T parse(final StringReader reader, final CommandContext<S> commandContext) throws CommandSyntaxException {
        return parse(reader);
    }
}
