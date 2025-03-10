package net.atlas.atlascore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.command.argument.AtlasConfigArgument;
import net.atlas.atlascore.command.argument.ConfigHolderArgument;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ConfigHolderLike;
import net.atlas.atlascore.config.ExtendedHolder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

import static net.atlas.atlascore.util.ComponentUtils.separatorLine;

public class ConfigCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(Commands.literal("atlas_config").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("reload").executes(ConfigCommand::reloadAll))
                .then(Commands.literal("read").executes(ConfigCommand::readAll))
                .then(Commands.literal("reset").executes(ConfigCommand::resetAll))
                .then(Commands.argument("config", AtlasConfigArgument.context(false))
                        .then(Commands.literal("reload").executes(context -> reload(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.literal("read").executes(context -> readConfig(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.literal("reset").executes(context -> resetConfig(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.argument("holder", ConfigHolderArgument.configHolderArgument("config"))
                                .then(Commands.literal("retrieve").executes(context -> readConfigHolder(context, AtlasConfigArgument.getConfig(context, "config"), ConfigHolderArgument.getConfigHolder(context, "holder"))))
                                .then(Commands.literal("edit")
                                        .then(Commands.argument("value", ConfigHolderArgument.ConfigValueArgument.configValueArgument("holder")).executes(context -> updateConfigValue(context, AtlasConfigArgument.getConfig(context, "config"), ConfigHolderArgument.getConfigHolder(context, "holder")))))
                                .then(Commands.literal("reset").executes(context -> resetConfigValue(context, AtlasConfigArgument.getConfig(context, "config"), ConfigHolderArgument.getConfigHolder(context, "holder")))))));
    }

    private static int readAll(CommandContext<CommandSourceStack> context) {
        for (AtlasConfig config : AtlasConfig.configs.values()) {
            readConfig(context, config);
        }
        return 1;
    }

    private static int readConfig(CommandContext<CommandSourceStack> context, AtlasConfig config) {
        createConfigInformation(config, context.getSource()::sendSystemMessage);
        return 1;
    }

    private static int readConfigHolder(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<?> configHolder) {
        context.getSource().sendSystemMessage(separatorLine(config.getFormattedName().copy(), true));
        if (configHolder instanceof ExtendedHolder extendedHolder) {
            context.getSource().sendSystemMessage(separatorLine(Component.translatable(configHolder.getAsHolder().getTranslationKey())));
            extendedHolder.fulfilListing(component -> context.getSource().sendSystemMessage(Component.literal("  » ").append(component)));
        } else if (!(configHolder instanceof AtlasConfig.ConfigHolder<?>)) {
            context.getSource().sendSystemMessage(separatorLine(Component.translatable(configHolder.getAsHolder().getTranslationKey())));
            ((ExtendedHolder)configHolder.getAsHolder()).listInner(configHolder.getName(), component -> context.getSource().sendSystemMessage(Component.literal("  » ").append(component)));
        } else context.getSource().sendSystemMessage(Component.literal("  » ").append(Component.translatable(configHolder.getAsHolder().getTranslationKey())).append(Component.literal(": ")).append(configHolder.getAsHolder().getValueAsComponent()));
        context.getSource().sendSystemMessage(separatorLine(null));
        return 1;
    }

    private static int resetAll(CommandContext<CommandSourceStack> context) {
        for (AtlasConfig config : AtlasConfig.configs.values()) {
            resetConfig(context, config);
        }
        return 1;
    }

    private static int resetConfig(CommandContext<CommandSourceStack> context, AtlasConfig config) {
        config.reset();
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, AtlasCore.AtlasConfigPacket.TYPE))
                player.connection.send(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(true, config)));
        }
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_config", config.getFormattedName())), true);
        context.getSource().sendSuccess(() -> separatorLine(null), true);
        return 1;
    }

    private static <T> int resetConfigValue(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<T> configHolder) {
        configHolder.resetValue();
        try {
            config.saveConfig();
        } catch (IOException e) {
            return 0;
        }
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, AtlasCore.AtlasConfigPacket.TYPE))
                player.connection.send(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(true, config)));
        }
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        if (!(configHolder instanceof AtlasConfig.ConfigHolder<T>)) {
            if (configHolder.getAsHolder().restartRequired.restartRequiredOn(FabricLoader.getInstance().getEnvironmentType())) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", ((ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
            else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_holder", ((ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()))), true);
            context.getSource().sendSuccess(() -> separatorLine(null), true);
            return 1;
        } else if (configHolder.getAsHolder().restartRequired.restartRequiredOn(FabricLoader.getInstance().getEnvironmentType())) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_holder", Component.translatable(configHolder.getAsHolder().getTranslationKey()))), true);
        context.getSource().sendSuccess(() -> separatorLine(null), true);
        return 1;
    }

    private static <T> int updateConfigValue(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<T> configHolder) {
        if (configHolder instanceof ExtendedHolder extendedHolder) {
            if (extendedHolder.getUnsetInners().isEmpty()) {
                return extendedHolder.postUpdate(context.getSource());
            } else {
                int ret = 1;
                for (ConfigHolderLike<?> inner : extendedHolder.getUnsetInners()) {
                    ret &= updateConfigValue(context, config, inner);
                }
                return ret;
            }
        }
        configHolder.setToParsedValue();
        try {
            config.saveConfig();
        } catch (IOException e) {
            return 0;
        }
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            if (ServerPlayNetworking.canSend(player, AtlasCore.AtlasConfigPacket.TYPE)) player.connection.send(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(true, config)));
        }
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        if (!(configHolder instanceof AtlasConfig.ConfigHolder<T>)) {
            if (configHolder.getAsHolder().restartRequired.restartRequiredOn(FabricLoader.getInstance().getEnvironmentType())) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", ((ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
            else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.update_holder", ((ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
        } else if (configHolder.getAsHolder().restartRequired.restartRequiredOn(FabricLoader.getInstance().getEnvironmentType())) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.update_holder", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        context.getSource().sendSuccess(() -> separatorLine(null), true);
        return 1;
    }

    private static void createConfigInformation(AtlasConfig config, Consumer<Component> sender) {
        sender.accept(separatorLine(config.getFormattedName().copy(), true));
        if (!config.categories.isEmpty()) {
            for (AtlasConfig.Category category : config.categories) {
                sender.accept(separatorLine(Component.translatable(category.translationKey())));
                for (AtlasConfig.ConfigHolder<?> configHolder : category.members()) {
                    if (configHolder instanceof ExtendedHolder extendedHolder) {
                        sender.accept(separatorLine(configHolder.getValueAsComponent().copy()));
                        extendedHolder.fulfilListing((component) -> sender.accept(Component.literal("  » ").append(component)));
                        sender.accept(separatorLine(null));
                    } else sender.accept(Component.literal("  » ").append(Component.translatable(configHolder.getTranslationKey())).append(Component.literal(": ")).append(configHolder.getValueAsComponent()));
                }
            }
        }
		List<AtlasConfig.ConfigHolder<?>> uncategorised = config.getUncategorisedHolders();
		if (!uncategorised.isEmpty()) {
            if (!config.categories.isEmpty()) sender.accept(separatorLine(Component.translatable("text.config.misc_category")));
            for (AtlasConfig.ConfigHolder<?> configHolder : config.getUncategorisedHolders()) {
                if (configHolder instanceof ExtendedHolder extendedHolder) {
                    sender.accept(separatorLine(configHolder.getValueAsComponent().copy()));
                    extendedHolder.fulfilListing((component) -> sender.accept(Component.literal("  » ").append(component)));
                    sender.accept(separatorLine(null));
                } else sender.accept(Component.literal("  » ").append(Component.translatable(configHolder.getTranslationKey())).append(Component.literal(": ")).append(configHolder.getValueAsComponent()));
            }
        }
        sender.accept(separatorLine(null));
    }

    private static int reloadAll(CommandContext<CommandSourceStack> context) {
        for (AtlasConfig config : AtlasConfig.configs.values()) {
            config.reload();
        }
        context.getSource().sendSuccess(() -> Component.translatable("commands.config.reload_all.success"), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context, AtlasConfig config) {
        config.reload();
        context.getSource().sendSuccess(() -> Component.translatable("commands.config.reload.success"), true);
        return 1;
    }
}
