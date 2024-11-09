package net.atlas.atlascore.init;

import net.atlas.atlascore.command.argument.AtlasConfigArgument;
import net.atlas.atlascore.command.argument.OptsArgument;
import net.atlas.atlascore.command.argument.ConfigHolderArgument;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraft.core.registries.BuiltInRegistries;

@SuppressWarnings("unused")
public class ArgumentInit {
    public static final ArgumentTypeInfo<AtlasConfigArgument, AtlasConfigArgument.Info.Template> ATLAS_CONFIG_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "atlas_config", AtlasConfigArgument.class, new AtlasConfigArgument.Info());
    public static final ArgumentTypeInfo<OptsArgument, OptsArgument.Info.Template> OPTS_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "opts_argument", OptsArgument.class, new OptsArgument.Info());
    public static final ArgumentTypeInfo<ConfigHolderArgument, ConfigHolderArgument.HolderInfo.Template> CONFIG_HOLDER_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "config_holder", ConfigHolderArgument.class, new ConfigHolderArgument.HolderInfo());
    public static final ArgumentTypeInfo<ConfigHolderArgument.ConfigValueArgument, ConfigHolderArgument.ConfigValueArgument.ValueInfo.Template> CONFIG_VALUE_ARGUMENT = ArgumentTypeInfos.register(BuiltInRegistries.COMMAND_ARGUMENT_TYPE, "config_value", ConfigHolderArgument.ConfigValueArgument.class, new ConfigHolderArgument.ConfigValueArgument.ValueInfo());
    public static void registerArguments() {

    }
}