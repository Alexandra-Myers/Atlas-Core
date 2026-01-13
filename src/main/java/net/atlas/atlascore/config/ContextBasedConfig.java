package net.atlas.atlascore.config;

import net.atlas.atlascore.util.Context;
import net.atlas.atlascore.util.ContextCondition;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public abstract class ContextBasedConfig extends AtlasConfig {
    final boolean isGeneric;
    public final ContextCondition contextCondition;
    final ContextBasedConfig generic;
    final List<ContextBasedConfig> subConfigs;

    @Deprecated
    public ContextBasedConfig(Identifier name, SyncMode defaultSyncMode, ConfigSide configSide, ContextCondition contextCondition) {
        super(name, defaultSyncMode, configSide);
        this.isGeneric = contextCondition.isGeneric();
        this.contextCondition = contextCondition;
        if (!isGeneric) {
            this.generic = (ContextBasedConfig) AtlasConfig.configs.get(name);
            this.subConfigs = null;
            this.generic.subConfigs.add(this);
        } else {
            this.generic = this;
            subConfigs = new ArrayList<>();
        }
    }

    public static <T extends ContextBasedConfig> T createGeneric(Class<T> tClass, Identifier name) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createGeneric(tClass, name, SyncMode.OVERRIDE_CLIENT, ConfigSide.COMMON);
    }

    public static <T extends ContextBasedConfig> T createGeneric(Class<T> tClass, Identifier name, ConfigSide configSide) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createGeneric(tClass, name, SyncMode.OVERRIDE_CLIENT, configSide);
    }

    public static <T extends ContextBasedConfig> T createGeneric(Class<T> tClass, Identifier name, SyncMode defaultSyncMode) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createGeneric(tClass, name, defaultSyncMode, ConfigSide.COMMON);
    }

    public static <T extends ContextBasedConfig> T createGeneric(Class<T> tClass, Identifier name, SyncMode defaultSyncMode, ConfigSide configSide) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return tClass.getConstructor(Identifier.class, SyncMode.class, ConfigSide.class, ContextCondition.class).newInstance(name, defaultSyncMode, configSide, ContextCondition.GENERIC);
    }

    public static <T extends ContextBasedConfig> T createContextual(Class<T> tClass, Identifier name, ContextCondition contextCondition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createContextual(tClass, name, SyncMode.OVERRIDE_CLIENT, ConfigSide.COMMON, contextCondition);
    }

    public static <T extends ContextBasedConfig> T createContextual(Class<T> tClass, Identifier name, ConfigSide configSide, ContextCondition contextCondition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createContextual(tClass, name, SyncMode.OVERRIDE_CLIENT, configSide, contextCondition);
    }

    public static <T extends ContextBasedConfig> T createContextual(Class<T> tClass, Identifier name, SyncMode defaultSyncMode, ContextCondition contextCondition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return createContextual(tClass, name, defaultSyncMode, ConfigSide.COMMON, contextCondition);
    }

    public static <T extends ContextBasedConfig> T createContextual(Class<T> tClass, Identifier name, SyncMode defaultSyncMode, ConfigSide configSide, ContextCondition contextCondition) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return tClass.getConstructor(Identifier.class, SyncMode.class, ConfigSide.class, ContextCondition.class).newInstance(name, defaultSyncMode, configSide, contextCondition);
    }

    @Override
    protected Path getConfigFolderPath() {
        if (isGeneric)
            return Path.of(FabricLoader.getInstance().getConfigDir().getFileName().getFileName() + "/" + name.getNamespace());
        return Path.of(FabricLoader.getInstance().getConfigDir().getFileName().getFileName() + "/" + name.getNamespace() + "/" + contextCondition.name());
    }
    public ContextBasedConfig getConfig(String name) {
        if (contextCondition.name().equals(name)) return this;
        if (subConfigs == null) return generic.getConfig(name);
        return subConfigs.stream().filter(contextBasedConfig -> contextBasedConfig.contextCondition.name().equals(name)).findFirst().orElse(null);
    }
    public ContextBasedConfig getConfig(Context context) {
        if (contextCondition.test(context))
            return this;
        if (subConfigs == null) return generic.getConfig(context);
        return subConfigs.stream().filter(contextBasedConfig -> contextBasedConfig.contextCondition.test(context)).findFirst().orElse(this);
    }
}
