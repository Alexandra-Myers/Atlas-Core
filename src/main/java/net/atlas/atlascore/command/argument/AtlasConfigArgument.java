package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.atlas.atlascore.util.Context;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record AtlasConfigArgument() {
    public static IdentifierArgument context() {
        return IdentifierArgument.id();
    }

    public static AtlasConfig getConfig(final CommandContext<?> context, String name, boolean requiresContext) {
        Identifier identifier = context.getArgument(name, Identifier.class);
        AtlasConfig ret = AtlasConfig.configs.get(identifier);
        if (requiresContext)
            ret = AtlasConfig.configs.values().stream().filter(config -> config instanceof ContextBasedConfig && Objects.equals(config.name, identifier)).findFirst().orElse(null);
        if (ret instanceof ContextBasedConfig contextBasedConfig) {
            if (context.getSource() instanceof CommandSourceStack sourceStack && sourceStack.getEntity() != null) ret = contextBasedConfig.getConfig(Context.builder().applyInformationFromCommandSourceStack(sourceStack).build());
            else if (context.getSource() instanceof CommandSourceStack sourceStack) ret = contextBasedConfig.getConfig(Context.builder().putOnDedicated(sourceStack.getServer().isDedicatedServer()).build());
        }
        return ret;
    }
    public static <S> CompletableFuture<Suggestions> suggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder, boolean requiresContext) {
        return SharedSuggestionProvider.suggestResource(AtlasConfig.configs.entrySet().stream().filter(entry -> entry.getValue().configSide.isCommon() && (!requiresContext || entry.getValue() instanceof ContextBasedConfig)).map(Map.Entry::getKey), suggestionsBuilder);
    }
}
