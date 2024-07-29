package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AtlasConfigArgument implements ArgumentType<AtlasConfig> {
    private static final Collection<String> EXAMPLES = List.of("foo:bar_config");

    public static AtlasConfigArgument atlasConfig() {
        return new AtlasConfigArgument();
    }

    public static AtlasConfig getConfig(final CommandContext<?> context, String name) {
        return context.getArgument(name, AtlasConfig.class);
    }

    public AtlasConfig parse(StringReader stringReader) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(stringReader);
        return AtlasConfig.configs.get(resourcelocation);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return SharedSuggestionProvider.suggestResource(AtlasConfig.configs.keySet(), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
