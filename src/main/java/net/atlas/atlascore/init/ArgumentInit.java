package net.atlas.atlascore.init;

import net.atlas.atlascore.command.argument.AtlasConfigArgument;
import net.atlas.atlascore.command.argument.ConfigHolderArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.core.registries.BuiltInRegistries;

@SuppressWarnings("unused")
public class ArgumentInit {
    public static final ArgumentTypeInfo<AtlasConfigArgument, SingletonArgumentInfo<AtlasConfigArgument>.Template> ATLAS_CONFIG_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "atlas_config", AtlasConfigArgument.class, SingletonArgumentInfo.contextFree(AtlasConfigArgument::atlasConfig));
    public static final ArgumentTypeInfo<ConfigHolderArgument, SingletonArgumentInfo<ConfigHolderArgument>.Template> CONFIG_HOLDER_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "config_holder", ConfigHolderArgument.class, SingletonArgumentInfo.contextFree(ConfigHolderArgument::configHolderArgument));
    public static final ArgumentTypeInfo<ConfigHolderArgument.ConfigValueArgument, SingletonArgumentInfo<ConfigHolderArgument.ConfigValueArgument>.Template> CONFIG_VALUE_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "config_value", ConfigHolderArgument.ConfigValueArgument.class, SingletonArgumentInfo.contextFree(ConfigHolderArgument.ConfigValueArgument::configValueArgument));
    public static void registerArguments() {

    }
}