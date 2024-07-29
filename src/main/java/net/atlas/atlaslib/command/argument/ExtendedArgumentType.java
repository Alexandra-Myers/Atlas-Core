package net.atlas.atlaslib.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

public interface ExtendedArgumentType<T> {
    <S> T parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder) throws CommandSyntaxException;
}
