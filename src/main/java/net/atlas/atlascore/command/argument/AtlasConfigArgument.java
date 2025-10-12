package net.atlas.atlascore.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.atlas.atlascore.util.Context;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record AtlasConfigArgument() {
    public static ResourceLocationArgument context() {
        return ResourceLocationArgument.id();
    }

    public static AtlasConfig getConfig(final CommandContext<?> context, String name, boolean requiresContext) {
        ResourceLocation resourcelocation = context.getArgument(name, ResourceLocation.class);
        AtlasConfig ret = AtlasConfig.configs.get(resourcelocation);
        if (requiresContext)
            ret = AtlasConfig.configs.values().stream().filter(config -> config instanceof ContextBasedConfig && Objects.equals(config.name, resourcelocation)).findFirst().orElse(null);
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
