package net.atlas.atlascore.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.buffer.ByteBuf;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.command.argument.AtlasConfigArgument;
import net.atlas.atlascore.command.argument.ConfigHolderArgument;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.ConfigHolderLike;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.IOException;
import java.util.function.Consumer;

public class ConfigCommand {
    public static void register(CommandDispatcher<CommandSourceStack> commandDispatcher) {
        commandDispatcher.register(Commands.literal("atlas_config").requires((commandSourceStack) -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("reload").executes(ConfigCommand::reloadAll))
                .then(Commands.literal("read").executes(ConfigCommand::readAll))
                .then(Commands.literal("reset").executes(ConfigCommand::resetAll))
                .then(Commands.argument("config", AtlasConfigArgument.atlasConfig())
                        .then(Commands.literal("reload").executes(context -> reload(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.literal("read").executes(context -> readConfig(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.literal("reset").executes(context -> resetConfig(context, AtlasConfigArgument.getConfig(context, "config"))))
                        .then(Commands.argument("holder", ConfigHolderArgument.configHolderArgument())
                                .then(Commands.literal("retrieve").executes(context -> readConfigHolder(context, AtlasConfigArgument.getConfig(context, "config"), ConfigHolderArgument.getConfigHolder(context, "holder"))))
                                .then(Commands.literal("edit")
                                        .then(Commands.argument("value", ConfigHolderArgument.ConfigValueArgument.configValueArgument()).executes(context -> updateConfigValue(context, AtlasConfigArgument.getConfig(context, "config"), ConfigHolderArgument.getConfigHolder(context, "holder")))))
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

    private static int readConfigHolder(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<?, ? extends ByteBuf> configHolder) {
        context.getSource().sendSystemMessage(separatorLine(config.getFormattedName().copy(), true));
        if (configHolder instanceof AtlasConfig.ExtendedHolder extendedHolder) {
            context.getSource().sendSystemMessage(separatorLine(Component.translatable(configHolder.getAsHolder().getTranslationKey())));
            extendedHolder.fulfilListing(component -> context.getSource().sendSystemMessage(Component.literal("  » ").append(component)));
        } else if (!(configHolder instanceof AtlasConfig.ConfigHolder<?, ? extends ByteBuf>)) {
            context.getSource().sendSystemMessage(separatorLine(Component.translatable(configHolder.getAsHolder().getTranslationKey())));
            ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).listInner(configHolder.getName(), component -> context.getSource().sendSystemMessage(Component.literal("  » ").append(component)));
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
        context.getSource().getServer().getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(config)));
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_config", config.getFormattedName())), true);
        return 1;
    }

    private static <T> int resetConfigValue(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<T, ? extends ByteBuf> configHolder) {
        configHolder.resetValue();
        try {
            config.saveConfig();
        } catch (IOException e) {
            return 0;
        }
        context.getSource().getServer().getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(config)));
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        if (!(configHolder instanceof AtlasConfig.ConfigHolder<T, ? extends ByteBuf>)) {
            if (configHolder.getAsHolder().restartRequired) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
            else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_holder", ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()))), true);
            return 1;
        } else if (configHolder.getAsHolder().restartRequired) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.reset_holder", Component.translatable(configHolder.getAsHolder().getTranslationKey()))), true);
        return 1;
    }

    private static <T> int updateConfigValue(CommandContext<CommandSourceStack> context, AtlasConfig config, ConfigHolderLike<T, ? extends ByteBuf> configHolder) throws CommandSyntaxException {
        if (configHolder instanceof AtlasConfig.ExtendedHolder) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
        }
        configHolder.setToParsedValue();
        try {
            config.saveConfig();
        } catch (IOException e) {
            return 0;
        }
        context.getSource().getServer().getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(config)));
        context.getSource().sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
        if (!(configHolder instanceof AtlasConfig.ConfigHolder<T, ? extends ByteBuf>)) {
            if (configHolder.getAsHolder().restartRequired) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
            else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.update_holder", ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerTranslation(configHolder.getName()), ((AtlasConfig.ExtendedHolder)configHolder.getAsHolder()).getInnerValue(configHolder.getName()))), true);
            return 1;
        } else if (configHolder.getAsHolder().restartRequired) context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        else context.getSource().sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.update_holder", Component.translatable(configHolder.getAsHolder().getTranslationKey()), configHolder.getAsHolder().getValueAsComponent())), true);
        return 1;
    }

    private static void createConfigInformation(AtlasConfig config, Consumer<Component> sender) {
        sender.accept(separatorLine(config.getFormattedName().copy(), true));
        for (AtlasConfig.Category category : config.categories) {
            sender.accept(separatorLine(Component.translatable(category.translationKey())));
            for (AtlasConfig.ConfigHolder<?, ? extends ByteBuf> configHolder : category.members()) {
                if (configHolder instanceof AtlasConfig.ExtendedHolder extendedHolder) {
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
            context.getSource().sendSuccess(() -> Component.translatable("commands.config.reload_all.success"), true);
        }
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> context, AtlasConfig config) {
        config.reload();
        context.getSource().sendSuccess(() -> Component.translatable("commands.config.reload.success"), true);
        return 1;
    }

    public static MutableComponent separatorLine(MutableComponent title) {
        return separatorLine(title, false);
    }
    public static MutableComponent separatorLine(MutableComponent title, boolean retainFormatting) {
        String spaces = "                                                                ";

        if (title != null) {
            int lineLength = spaces.length() - Math.round((float)title.getString().length() * 1.33F) - 4;
            title = retainFormatting ? title.withStyle(title.getStyle().withStrikethrough(false)) : title.withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.configNameDisplayColour.get())).withStrikethrough(false));
            return Component.literal(spaces.substring(0, lineLength / 2)).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true))
                    .append(Component.literal("[ ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(false)))
                    .append(title)
                    .append(Component.literal(" ]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(false)))
                    .append(Component.literal(spaces.substring(0, (lineLength + 1) / 2))).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true));
        } else {
            return Component.literal(spaces).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(AtlasCore.CONFIG.grayFormattingColour.get())).withStrikethrough(true));
        }
    }
}
