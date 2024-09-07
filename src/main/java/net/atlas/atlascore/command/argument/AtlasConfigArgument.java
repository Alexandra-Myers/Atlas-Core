package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.util.Context;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record AtlasConfigArgument(boolean requiresContext) implements ArgumentType<AtlasConfig> {
    private static final Collection<String> EXAMPLES = List.of("foo:bar_config");

    public static AtlasConfigArgument noContext() {
        return new AtlasConfigArgument(false);
    }
    public static AtlasConfigArgument context() {
        return new AtlasConfigArgument(true);
    }

    public static AtlasConfig getConfig(final CommandContext<?> context, String name) {
        AtlasConfig ret = context.getArgument(name, AtlasConfig.class);
        if (ret instanceof ContextBasedConfig contextBasedConfig) {
            if (context.getSource() instanceof CommandSourceStack sourceStack && sourceStack.getEntity() != null) ret = contextBasedConfig.getConfig(Context.builder().applyInformationFromCommandSourceStack(sourceStack).build());
            else if (context.getSource() instanceof CommandSourceStack sourceStack) ret = contextBasedConfig.getConfig(Context.builder().putOnDedicated(sourceStack.getServer().isDedicatedServer()).build());
        }
        return ret;
    }

    public AtlasConfig parse(StringReader stringReader) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(stringReader);
        if (requiresContext)
            return AtlasConfig.configs.values().stream().filter(config -> config instanceof ContextBasedConfig && Objects.equals(config.name, resourcelocation)).findFirst().orElse(null);
        return AtlasConfig.configs.get(resourcelocation);
    }

    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        return SharedSuggestionProvider.suggestResource(AtlasConfig.configs.entrySet().stream().filter(entry -> entry.getValue().configSide.existsOnServer() && (!requiresContext || entry.getValue() instanceof ContextBasedConfig)).map(Map.Entry::getKey), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
