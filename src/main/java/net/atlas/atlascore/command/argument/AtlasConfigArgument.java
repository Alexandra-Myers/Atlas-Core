package net.atlas.atlascore.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.atlas.atlascore.util.Context;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public record AtlasConfigArgument(boolean requiresContext) implements ArgumentType<AtlasConfig> {
    private static final Collection<String> EXAMPLES = List.of("foo:bar_config");

    public static AtlasConfigArgument context(boolean requiresContext) {
        return new AtlasConfigArgument(requiresContext);
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
        return SharedSuggestionProvider.suggestResource(AtlasConfig.configs.entrySet().stream().filter(entry -> entry.getValue().configSide.isCommon() && (!requiresContext || entry.getValue() instanceof ContextBasedConfig)).map(Map.Entry::getKey), suggestionsBuilder);
    }

    public Collection<String> getExamples() {
        return EXAMPLES;
    }
    public static class Info implements ArgumentTypeInfo<AtlasConfigArgument, Info.Template> {
        public void serializeToNetwork(Info.Template template, FriendlyByteBuf friendlyByteBuf) {
            friendlyByteBuf.writeBoolean(template.requiresContext);
        }

        public void serializeToJson(Info.Template template, JsonObject jsonObject) {
            jsonObject.addProperty("requiresContext", template.requiresContext);
        }

        public Info.Template deserializeFromNetwork(FriendlyByteBuf friendlyByteBuf) {
            return new Template(friendlyByteBuf.readBoolean());
        }

        public Info.Template unpack(AtlasConfigArgument argumentType) {
            return new Template(argumentType.requiresContext);
        }

        public final class Template implements ArgumentTypeInfo.Template<AtlasConfigArgument> {
            private final boolean requiresContext;

            public Template(final boolean requiresContext) {
                this.requiresContext = requiresContext;
            }

            public AtlasConfigArgument instantiate(CommandBuildContext commandBuildContext) {
                return context(requiresContext);
            }

            public ArgumentTypeInfo<AtlasConfigArgument, ?> type() {
                return Info.this;
            }
        }
    }
}
