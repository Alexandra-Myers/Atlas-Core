package net.atlas.atlascore.config;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.netty.buffer.ByteBuf;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.*;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.command.argument.ConfigHolderArgument;
import net.atlas.atlascore.util.ConfigRepresentable;
import net.atlas.atlascore.util.JavaToJSONSerialisation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

import static net.atlas.atlascore.command.OptsArgumentUtils.SUGGEST_NOTHING;
import static net.atlas.atlascore.util.ComponentUtils.separatorLine;

@SuppressWarnings("ALL")
public abstract class AtlasConfig {
    public final ResourceLocation name;
    public final SyncMode defaultSyncMode;
    public final ConfigSide configSide;
    public boolean isDefault;
    public final Map<String, ConfigHolder<?, ? extends ByteBuf>> valueNameToConfigHolderMap = Maps.newHashMap();
	public final List<Category> categories;
    public static final Map<ResourceLocation, AtlasConfig> configs = Maps.newHashMap();
	public static final Map<String, AtlasConfig> menus = Maps.newHashMap();
    File configFile;
    JsonObject configJsonObject;
    List<ObjectHolder<?>> objectValues;
    List<EnumHolder<?>> enumValues;
    List<StringHolder> stringValues;
    List<BooleanHolder> booleanValues;
    List<IntegerHolder> integerValues;
    List<DoubleHolder> doubleValues;
    List<ColorHolder> colorValues;

    public AtlasConfig(ResourceLocation name, SyncMode defaultSyncMode, ConfigSide configSide) {
        this.configSide = configSide;
        this.defaultSyncMode = defaultSyncMode;
        this.name = name;
        objectValues = new ArrayList<>();
        enumValues = new ArrayList<>();
        stringValues = new ArrayList<>();
        booleanValues = new ArrayList<>();
        integerValues = new ArrayList<>();
        doubleValues = new ArrayList<>();
        colorValues = new ArrayList<>();
        categories = createCategories();
        defineConfigHolders();
        if (!Files.exists(getConfigFolderPath()))
            try {
                Files.createDirectories(getConfigFolderPath());
            } catch (IOException e) {
                throw new ReportedException(new CrashReport("Failed to create config directory for config " + name, e));
            }

        load();
        if (!configs.containsKey(name)) configs.put(name, this);
    }
    public AtlasConfig(ResourceLocation name, SyncMode defaultSyncMode) {
        this(name, defaultSyncMode, ConfigSide.COMMON);
    }
    public AtlasConfig(ResourceLocation name, ConfigSide configSide) {
        this(name, SyncMode.OVERRIDE_CLIENT, configSide);
    }
    public AtlasConfig(ResourceLocation name) {
        this(name, SyncMode.OVERRIDE_CLIENT, ConfigSide.COMMON);
    }

    public Component getFormattedName() {
        return Component.translatable("text.config." + name.getPath() + ".title");
    }

	public AtlasConfig declareDefaultForMod(String modID) {
		menus.put(modID, this);
		return this;
	}

	public @NotNull List<Category> createCategories() {
		return new ArrayList<>();
	}

	public abstract void defineConfigHolders();

	public abstract void resetExtraHolders();

    public abstract <T> void alertChange(ConfigValue<T> tConfigValue, T newValue);

    public abstract <T> void alertClientValue(ConfigValue<T> tConfigValue, T serverValue, T clientValue);

    public static String getString(JsonObject element, String name) {
        return element.get(name).getAsString();
    }

    public static Integer getInt(JsonObject element, String name) {
        return element.get(name).getAsInt();
    }

    public static Double getDouble(JsonObject element, String name) {
        return element.get(name).getAsDouble();
    }
    public static Boolean getBoolean(JsonObject element, String name) {
        return element.get(name).getAsBoolean();
    }

    public static Integer getColor(JsonObject element, String name, ColorHolder colorHolder) {
        String color = stripHexStarter(getString(element, name));
        if (color.length() > 8)
            return colorHolder.get();
        if (!colorHolder.hasAlpha && color.length() > 6)
            return colorHolder.get();
        return (int) Long.parseLong(color, 16);
    }

    public static String stripHexStarter(String hex) {
        return hex.startsWith("#") ? hex.substring(1) : hex;
    }
    @ApiStatus.Internal
    protected Path getConfigFolderPath() {
        return Path.of(FabricLoader.getInstance().getConfigDir().getFileName().getFileName() + "/" + name.getNamespace() + configSide.getAsDir());
    }
	public void reload() {
		resetExtraHolders();
		load();
	}
    @ApiStatus.Internal
    protected void load() {
		isDefault = false;
        configFile = new File(getConfigFolderPath().toAbsolutePath() + "/" + name.getPath() + ".json");
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                try (InputStream inputStream = getDefaultedConfig()) {
                    Files.write(configFile.toPath(), inputStream.readAllBytes());
                }
            } catch (IOException e) {
                throw new ReportedException(new CrashReport("Failed to create config file for config " + name, e));
            }
        }

        try {
            configJsonObject = JsonParser.parseReader(new JsonReader(new FileReader(configFile))).getAsJsonObject();
            for (ObjectHolder<?> objectHolder : objectValues)
                if (configJsonObject.has(objectHolder.heldValue.name))
                    objectHolder.setFromJSONObjectAndResetManaged(configJsonObject.getAsJsonObject(objectHolder.heldValue.name));
            for (EnumHolder<?> enumHolder : enumValues)
                if (configJsonObject.has(enumHolder.heldValue.name))
                    enumHolder.setValueAndResetManaged(getString(configJsonObject, enumHolder.heldValue.name));
            for (StringHolder stringHolder : stringValues)
                if (configJsonObject.has(stringHolder.heldValue.name))
                    stringHolder.setValueAndResetManaged(getString(configJsonObject, stringHolder.heldValue.name));
            for (BooleanHolder booleanHolder : booleanValues)
                if (configJsonObject.has(booleanHolder.heldValue.name))
                    booleanHolder.setValueAndResetManaged(getBoolean(configJsonObject, booleanHolder.heldValue.name));
            for (IntegerHolder integerHolder : integerValues)
                if (configJsonObject.has(integerHolder.heldValue.name))
                    integerHolder.setValueAndResetManaged(getInt(configJsonObject, integerHolder.heldValue.name));
            for (DoubleHolder doubleHolder : doubleValues)
                if (configJsonObject.has(doubleHolder.heldValue.name))
                    doubleHolder.setValueAndResetManaged(getDouble(configJsonObject, doubleHolder.heldValue.name));
            for (ColorHolder colorHolder : colorValues)
                if (configJsonObject.has(colorHolder.heldValue.name))
                    colorHolder.setValueAndResetManaged(getColor(configJsonObject, colorHolder.heldValue.name, colorHolder));
            loadExtra(configJsonObject);
        } catch (IOException | IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected abstract void loadExtra(JsonObject jsonObject);
    protected abstract InputStream getDefaultedConfig();
    public AtlasConfig loadFromNetwork(RegistryFriendlyByteBuf buf) {
        objectValues.forEach(objectHolder -> objectHolder.readFromBuf(buf));
        enumValues.forEach(enumHolder -> enumHolder.readFromBuf(buf));
        stringValues.forEach(stringHolder -> stringHolder.readFromBuf(buf));
        booleanValues.forEach(booleanHolder -> booleanHolder.readFromBuf(buf));
        integerValues.forEach(integerHolder -> integerHolder.readFromBuf(buf));
        doubleValues.forEach(doubleHolder -> doubleHolder.readFromBuf(buf));
        colorValues.forEach(colorHolder -> colorHolder.readFromBuf(buf));
        return this;
    }
    public static AtlasConfig staticLoadFromNetwork(RegistryFriendlyByteBuf buf) {
        return configs.get(buf.readResourceLocation()).loadFromNetwork(buf);
    }

    public static AtlasConfig staticReadClientConfigInformation(RegistryFriendlyByteBuf buf) {
        return configs.get(buf.readResourceLocation()).readClientConfigInformation(buf);
    }

    public AtlasConfig readClientConfigInformation(RegistryFriendlyByteBuf buf) {
        objectValues.forEach(objectHolder -> objectHolder.broadcastClientValueRecieved(buf));
        enumValues.forEach(enumHolder -> enumHolder.broadcastClientValueRecieved(buf));
        stringValues.forEach(stringHolder -> stringHolder.broadcastClientValueRecieved(buf));
        booleanValues.forEach(booleanHolder -> booleanHolder.broadcastClientValueRecieved(buf));
        integerValues.forEach(integerHolder -> integerHolder.broadcastClientValueRecieved(buf));
        doubleValues.forEach(doubleHolder -> doubleHolder.broadcastClientValueRecieved(buf));
        colorValues.forEach(colorHolder -> colorHolder.broadcastClientValueRecieved(buf));
        return this;
    }

    public void saveToNetwork(RegistryFriendlyByteBuf buf) {
        objectValues.forEach(objectHolder -> objectHolder.writeToBuf(buf));
        enumValues.forEach(enumHolder -> enumHolder.writeToBuf(buf));
        stringValues.forEach(stringHolder -> stringHolder.writeToBuf(buf));
        booleanValues.forEach(booleanHolder -> booleanHolder.writeToBuf(buf));
        integerValues.forEach(integerHolder -> integerHolder.writeToBuf(buf));
        doubleValues.forEach(doubleHolder -> doubleHolder.writeToBuf(buf));
        colorValues.forEach(colorHolder -> colorHolder.writeToBuf(buf));
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
    public ConfigHolder<?, ? extends ByteBuf> fromValue(ConfigValue<?> value) {
        return valueNameToConfigHolderMap.get(value.name);
    }

    public <T extends ConfigRepresentable> ObjectHolder<T> createObject(String name, T defaultInstance, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
        return createObject(name, defaultInstance, clazz, serialisation, streamCodec, true, defaultSyncMode);
    }
    public <T extends ConfigRepresentable> ObjectHolder<T> createObject(String name, T defaultInstance, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, boolean expandByDefault) {
        return createObject(name, defaultInstance, clazz, serialisation, streamCodec, expandByDefault, defaultSyncMode);
    }

    public <T extends ConfigRepresentable> ObjectHolder<T> createObject(String name, T defaultInstance, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, SyncMode syncMode) {
        return createObject(name, defaultInstance, clazz, serialisation, streamCodec, true, syncMode);
    }
    public <T extends ConfigRepresentable> ObjectHolder<T> createObject(String name, T defaultInstance, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, boolean expandByDefault, SyncMode syncMode) {
        ObjectHolder<T> objectHolder = new ObjectHolder<>(new ConfigValue<>(defaultInstance, null, false, name, this, syncMode), clazz, serialisation, streamCodec, expandByDefault);
        objectValues.add(objectHolder);
        return objectHolder;
    }
    public final <E extends Enum<E>> EnumHolder<E> createEnum(String name, E defaultVal, Class<E> clazz, E[] values, Function<Enum, Component> names) {
        return createEnum(name, defaultVal, clazz, values, names, defaultSyncMode);
    }
    public final <E extends Enum<E>> EnumHolder<E> createEnum(String name, E defaultVal, Class<E> clazz, E[] values, Function<Enum, Component> names, SyncMode syncMode) {
        EnumHolder<E> enumHolder = new EnumHolder<>(new ConfigValue<>(defaultVal, values, false, name, this, syncMode), clazz, names);
        enumValues.add(enumHolder);
        return enumHolder;
    }
    public StringHolder createStringRange(String name, String defaultVal, String... values) {
        return createStringRange(name, defaultVal, defaultSyncMode, values);
    }
    public StringHolder createStringRange(String name, String defaultVal, SyncMode syncMode, String... values) {
        StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, values, false, name, this, syncMode));
        stringValues.add(stringHolder);
        return stringHolder;
    }
    public StringHolder createString(String name, String defaultVal) {
        return createString(name, defaultVal, defaultSyncMode);
    }
    public StringHolder createString(String name, String defaultVal, SyncMode syncMode) {
        StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, null, false, name, this, syncMode));
        stringValues.add(stringHolder);
        return stringHolder;
    }
    public BooleanHolder createBoolean(String name, boolean defaultVal) {
        return createBoolean(name, defaultVal, defaultSyncMode);
    }
    public BooleanHolder createBoolean(String name, boolean defaultVal, SyncMode syncMode) {
        BooleanHolder booleanHolder = new BooleanHolder(new ConfigValue<>(defaultVal, new Boolean[]{false, true}, false, name, this, syncMode));
        booleanValues.add(booleanHolder);
        return booleanHolder;
    }
    public ColorHolder createColor(String name, Integer defaultVal, boolean alpha) {
        return createColor(name, defaultVal, alpha, defaultSyncMode);
    }
    public ColorHolder createColor(String name, Integer defaultVal, boolean alpha, SyncMode syncMode) {
        ColorHolder colorHolder = new ColorHolder(new ConfigValue<>(defaultVal, null, false, name, this, defaultSyncMode), alpha);
        colorValues.add(colorHolder);
        return colorHolder;
    }
    public IntegerHolder createIntegerUnbound(String name, Integer defaultVal) {
        return createInteger(name, defaultVal, null, false, false, defaultSyncMode);
    }
    public IntegerHolder createIntegerUnbound(String name, Integer defaultVal, SyncMode syncMode) {
        return createInteger(name, defaultVal, null, false, false, syncMode);
    }
    public IntegerHolder createInRestrictedValues(String name, Integer defaultVal, Integer... values) {
        return createInteger(name, defaultVal, values, false, false, defaultSyncMode);
    }
    public IntegerHolder createInRestrictedValues(String name, Integer defaultVal, SyncMode syncMode, Integer... values) {
        return createInteger(name, defaultVal, values, false, false, syncMode);
    }
    public IntegerHolder createInRange(String name, int defaultVal, int min, int max, boolean isSlider) {
        return createInRange(name, defaultVal, min, max, isSlider, defaultSyncMode);
    }
    public IntegerHolder createInRange(String name, int defaultVal, int min, int max, boolean isSlider, SyncMode syncMode) {
        Integer[] range = new Integer[]{min, max};
        return createInteger(name, defaultVal, range, true, isSlider, syncMode);
    }
    public IntegerHolder createInteger(String name, Integer defaultVal, Integer[] values, boolean isRange, boolean isSlider, SyncMode syncMode) {
        IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, values, isRange, name, this, syncMode), isSlider);
        integerValues.add(integerHolder);
        return integerHolder;
    }
    public DoubleHolder createDoubleUnbound(String name, Double defaultVal) {
        return createDouble(name, defaultVal, null, false, defaultSyncMode);
    }
    public DoubleHolder createDoubleUnbound(String name, Double defaultVal, SyncMode syncMode) {
        return createDouble(name, defaultVal, null, false, syncMode);
    }
    public DoubleHolder createInRestrictedValues(String name, Double defaultVal, Double... values) {
        return createDouble(name, defaultVal, values, false, defaultSyncMode);
    }
    public DoubleHolder createInRestrictedValues(String name, Double defaultVal, SyncMode syncMode, Double... values) {
        return createDouble(name, defaultVal, values, false, syncMode);
    }
    public DoubleHolder createInRange(String name, double defaultVal, double min, double max) {
        return createInRange(name, defaultVal, min, max, defaultSyncMode);
    }
    public DoubleHolder createInRange(String name, double defaultVal, double min, double max, SyncMode syncMode) {
        Double[] range = new Double[]{min, max};
        return createDouble(name, defaultVal, range, true, syncMode);
    }
    public DoubleHolder createDouble(String name, Double defaultVal, Double[] values, boolean isRange, SyncMode syncMode) {
        DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, values, isRange, name, this, syncMode));
        doubleValues.add(doubleHolder);
        return doubleHolder;
    }

	public final void saveConfig() throws IOException {
		PrintWriter printWriter = new PrintWriter(new FileWriter(configFile), true);
		JsonWriter jsonWriter = new JsonWriter(printWriter);
		jsonWriter.beginObject();
		jsonWriter.setIndent("\t");
		saveExtra(jsonWriter, printWriter);
		for (Category category : categories) {
			for (ConfigHolder<?, ? extends ByteBuf> holder : category.members) {
				holder.writeToJSONFile(jsonWriter);
				printWriter.flush();
			}
			printWriter.println();
		}
		printWriter.write("}");
		printWriter.close();
	}

	public abstract void saveExtra(JsonWriter jsonWriter, PrintWriter printWriter);

    public enum ConfigSide {
        CLIENT,
        SERVER,
        COMMON;

        public String getAsDir() {
            return switch(this) {
                case COMMON -> "";
                case CLIENT -> "/client";
                case SERVER -> "/server";
            };
        }
        public boolean existsOnServer() {
            return this != CLIENT;
        }
        public boolean existsOnClient() {
            return this != SERVER;
        }
        public boolean isSided() {
            return this != COMMON;
        }
    }

    public enum SyncMode {
        NONE,
        INFORM_SERVER,
        OVERRIDE_CLIENT
    }

    public enum RestartRequiredMode {
        NO_RESTART(env -> false),
        RESTART_CLIENT(env -> env == EnvType.CLIENT),
        RESTART_BOTH(env -> true);
        public final Predicate<EnvType> forEnvironment;

        RestartRequiredMode(Predicate<EnvType> predicate) {
            forEnvironment = predicate;
        }
        
        public boolean restartRequiredOn(EnvType envType) {
            return forEnvironment.test(envType);
        }
    }
	public record ConfigValue<T>(T defaultValue, T[] possibleValues, boolean isRange, String name, AtlasConfig owner, SyncMode syncMode) {
        public void emitChanged(T newValue) {
            owner.alertChange(this, newValue);
        }

        public void emitClientValueRecieved(T serverValue, T clientValue) {
            owner.alertClientValue(this, serverValue, clientValue);
        }
        public boolean isValid(T newValue) {
            return possibleValues == null || Arrays.stream(possibleValues).toList().contains(newValue);
        }
        public void addAssociation(ConfigHolder<T, ? extends ByteBuf> configHolder) {
            if (owner.valueNameToConfigHolderMap.containsKey(name))
                throw new ReportedException(new CrashReport("Tried to associate a ConfigHolder to a ConfigValue which already has one!", new RuntimeException()));
            owner.valueNameToConfigHolderMap.put(name, configHolder);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ConfigValue<?> that)) return false;
            return isRange() == that.isRange() && Objects.equals(defaultValue, that.defaultValue) && Arrays.equals(possibleValues, that.possibleValues) && Objects.equals(name, that.name) && Objects.equals(owner, that.owner);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(defaultValue, isRange(), name, owner);
            result = 31 * result + Arrays.hashCode(possibleValues);
            return result;
        }
    }

    public static abstract class ConfigHolder<T, B extends ByteBuf> implements ConfigHolderLike<T, B> {
        protected T value;
        protected T prevValue = null;
        protected T synchedValue = null;
        protected T parsedValue = null;
        public final ConfigValue<T> heldValue;
        public final StreamCodec<B, T> codec;
		public final BiConsumer<JsonWriter, T> update;
		public RestartRequiredMode restartRequired = RestartRequiredMode.NO_RESTART;
		public boolean serverManaged = false;
		public Supplier<Optional<Component[]>> tooltip = Optional::empty;

		public ConfigHolder(ConfigValue<T> value, StreamCodec<B, T> codec, BiConsumer<JsonWriter, T> update) {
            this.value = value.defaultValue;
            heldValue = value;
            if (codec != null) this.codec = codec;
            else this.codec = formAlternateCodec();
            value.addAssociation(this);
			this.update = update;
        }

        protected StreamCodec<B, T> formAlternateCodec() {
            return null;
        }

        public T get() {
            if (synchedValue != null)
                return synchedValue;
            return value;
        }
        public boolean wasUpdated() {
            return synchedValue != null;
        }
		public void writeToJSONFile(JsonWriter writer) throws IOException {
			writer.name(heldValue.name);
			update.accept(writer, value);
		}
        public void writeToBuf(B buf) {
            if (heldValue.syncMode() != SyncMode.NONE)
                codec.encode(buf, value);
        }
        public void readFromBuf(B buf) {
            if (heldValue.syncMode() != SyncMode.NONE) {
                T newValue = codec.decode(buf);
                if (isNotValid(newValue) || heldValue.syncMode == SyncMode.INFORM_SERVER)
                    return;
                if (Objects.equals(newValue, value)) {
                    serverManaged = heldValue.owner.configSide.existsOnServer();
                    return;
                }
                setSynchedValue(newValue);
            }
        }
        public void broadcastClientValueRecieved(B buf) {
            if (heldValue.syncMode() != SyncMode.NONE) {
                T clientValue = codec.decode(buf);
                heldValue.emitClientValueRecieved(value, clientValue);
            }
        }
        public boolean isNotValid(T newValue) {
            return !heldValue.isValid(newValue);
        }
		public void setValueAndResetManaged(T newValue) {
			setValue(newValue);
			serverManaged = false;
            synchedValue = null;
		}
        public void setValue(T newValue) {
            if (isNotValid(newValue))
                return;
            heldValue.emitChanged(newValue);
            prevValue = value;
            value = newValue;
        }
        public void setSynchedValue(T newValue) {
            if (isNotValid(newValue))
                return;
            heldValue.emitChanged(newValue);
            synchedValue = newValue;
            serverManaged = heldValue.owner.configSide.existsOnServer();
        }

        @Override
        public void resetValue() {
            setValue(heldValue.defaultValue());
        }

        public void tieToCategory(Category category) {
			category.addMember(this);
		}
		public void setRestartRequired(RestartRequiredMode restartRequired) {
			this.restartRequired = restartRequired;
		}
		public void setupTooltip(int length) {
			Component[] components = new Component[length];
			for (int i = 0; i < length; i++) {
				components[i] = Component.translatable(getTranslationKey() + ".tooltip." + i);
			}
			this.tooltip = () -> Optional.of(components);
		}
		public String getTranslationKey() {
			return "text.config." + heldValue.owner.name.getPath() + ".option." + heldValue.name;
		}
		public String getTranslationResetKey() {
			return "text.config." + heldValue.owner.name.getPath() + ".reset";
		}

        public abstract Component getValueAsComponent();

        @Environment(EnvType.CLIENT)
		public abstract AbstractConfigListEntry<?> transformIntoConfigEntry();

        @Override
        public void setToParsedValue() {
            if (parsedValue == null)
                return;
            setValue(parsedValue);
            parsedValue = null;
        }

        @Override
        public boolean hasParsedValue() {
            return parsedValue != null;
        }

        @Override
        public ConfigHolder<T, B> getAsHolder() {
            return this;
        }

        @Override
        public String getName() {
            return heldValue.name;
        }

        // Super dangerous, only use when certain
        public final void setValueAmbiguousType(Object o) {
            setValue((T) o);
        }

        public void setToPreviousValue() {
            if (prevValue != null) setValue(prevValue);
        }

        public void setToSynchedValue() {
            if (synchedValue != null) setValue(synchedValue);
        }
    }
    public static interface ExtendedHolder {
        Component getInnerTranslation(String name);
        Component getInnerValue(String name);
        void listInner(String name, Consumer<Component> input);
        void fulfilListing(Consumer<Component> input);
        List<ConfigHolderLike<?, ?>> getUnsetInners();
        ConfigHolderLike<?, ? extends ByteBuf> findInner(StringReader reader) throws CommandSyntaxException;
        ConfigHolderLike<?, ? extends ByteBuf> retrieveInner(String name) throws CommandSyntaxException;
        CompletableFuture<Suggestions> suggestInner(StringReader reader, SuggestionsBuilder builder);
        int postUpdate(CommandSourceStack commandSourceStack);
    }
    public static class ObjectHolder<T extends ConfigRepresentable> extends ConfigHolder<T, RegistryFriendlyByteBuf> implements ExtendedHolder {
        public final Class<T> clazz;
        public final JavaToJSONSerialisation<T> serialisation;
        public final boolean expandByDefault;

        private ObjectHolder(ConfigValue<T> value, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> sync, boolean expandByDefault) {
            super(value, sync, (writer, t) -> {
                try {
                    writer.beginObject();
                    serialisation.encoder().accept(writer, t);
                    writer.endObject();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            this.value.setOwnerHolder(this);
            this.clazz = clazz;
            this.serialisation = serialisation;
            this.expandByDefault = expandByDefault;
        }

        public void setSynchedFromJSONObject(JsonObject object) {
            setSynchedValue(serialisation.decoder().apply(this, object));
        }

        public void setFromJSONObjectAndResetManaged(JsonObject object) {
            setValueAndResetManaged(serialisation.decoder().apply(this, object));
        }

        @Override
        public Component getValueAsComponent() {
            return Component.translatable(getTranslationKey());
        }

        @Override
        @Environment(EnvType.CLIENT)
        public AbstractConfigListEntry<?> transformIntoConfigEntry() {
            return new MultiElementListEntry<>(Component.translatable(getTranslationKey()), get(), get().transformIntoConfigEntries(), expandByDefault);
        }

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            StringReader reader = new StringReader(builder.getInput());
            reader.setCursor(builder.getStart());
            SuggestionsVisitor visitor = new SuggestionsVisitor();
            visitor.visitSuggestions(suggestionsBuilder -> SharedSuggestionProvider.suggest(heldValue.defaultValue.fields(), builder));
            try {
                suggestFields(visitor, commandContext, reader);
            } catch (CommandSyntaxException ignored) {

            }
            return visitor.resolveSuggestions(builder, reader);
        }

        private <S> void suggestFields(SuggestionsVisitor visitor, CommandContext<S> context, StringReader reader) throws CommandSyntaxException {
            int cursor = reader.getCursor();
            String fieldName = ConfigHolderArgument.readHolderName(reader);
            if (retrieveInner(fieldName) == null) {
                reader.setCursor(cursor);
                throw ConfigHolderArgument.ERROR_UNKNOWN_HOLDER.createWithContext(reader, fieldName);
            }
            visitor.visitSuggestions(this::suggestSetValue);
            reader.expect('=');
            visitor.visitSuggestions(builder -> {
                try {
                    return retrieveInner(fieldName).buildSuggestions(context, builder);
                } catch (CommandSyntaxException e) {
                    reader.setCursor(cursor);
                    return Suggestions.empty();
                }
            });
        }

        private CompletableFuture<Suggestions> suggestSetValue(SuggestionsBuilder suggestionsBuilder) {
            if (suggestionsBuilder.getRemaining().isEmpty()) {
                suggestionsBuilder.suggest(String.valueOf('='));
            }

            return suggestionsBuilder.buildFuture();
        }

        static class SuggestionsVisitor {
            private Function<SuggestionsBuilder, CompletableFuture<Suggestions>> suggestions = SUGGEST_NOTHING;

            public void visitSuggestions(Function<SuggestionsBuilder, CompletableFuture<Suggestions>> function) {
                this.suggestions = function;
            }

            public CompletableFuture<Suggestions> resolveSuggestions(SuggestionsBuilder suggestionsBuilder, StringReader stringReader) {
                return this.suggestions.apply(suggestionsBuilder.createOffset(stringReader.getCursor()));
            }
        }

        @Override
        public <S> T parse(StringReader stringReader, S source, CommandContext<S> context) throws CommandSyntaxException {
            int cursor = stringReader.getCursor();
            try {
                FieldHolder field = (FieldHolder) findInner(stringReader);
                stringReader.expect('=');
                field.parse(stringReader, source, context);
                field.setToParsedValue();
            } catch (CommandSyntaxException e) {
                stringReader.setCursor(cursor);
                throw e;
            }
            return get();
        }

        @Override
        public Component getInnerTranslation(String name) {
            return get().getFieldName(name);
        }

        @Override
        public Component getInnerValue(String name) {
            return get().getFieldValue(name);
        }

        @Override
        public void listInner(String name, Consumer<Component> input) {
            get().listField(name, input);
        }

        @Override
        public void fulfilListing(Consumer<Component> input) {
            get().listFields(input);
        }

        @Override
        public List<ConfigHolderLike<?, ?>> getUnsetInners() {
            List<ConfigHolderLike<?, ?>> inners = new ArrayList<>();
            List<String> fields = get().fields();
            for (String field : fields) {
                try {
                    ConfigHolderLike<?, ? extends ByteBuf> fieldHolder = retrieveInner(field);
                    if (fieldHolder.hasParsedValue()) inners.add(fieldHolder);
                } catch (CommandSyntaxException e) {

                }
            }
            return inners;
        }

        @Override
        public ConfigHolderLike<?, ? extends ByteBuf> findInner(StringReader reader) throws CommandSyntaxException {
            List<String> fields = heldValue.defaultValue().fields();
            String name = ConfigHolderArgument.readHolderName(reader);
            if (!fields.contains(name))
                throw ConfigHolderArgument.ERROR_UNKNOWN_HOLDER.createWithContext(reader, name);
            Field field = heldValue.defaultValue().fieldRepresentingHolder(name);
            try {
                return new FieldHolder(this, field, heldValue.defaultValue().argumentTypeRepresentingHolder(name), field.get(heldValue.defaultValue()), name);
            } catch (IllegalAccessException e) {
                throw ConfigHolderArgument.ERROR_MALFORMED_HOLDER.createWithContext(reader, name);
            }
        }

        @Override
        public ConfigHolderLike<?, ? extends ByteBuf> retrieveInner(String name) throws CommandSyntaxException {
            List<String> fields = heldValue.defaultValue().fields();
            if (!fields.contains(name))
                return null;
            Field field = heldValue.defaultValue().fieldRepresentingHolder(name);
            try {
                return new FieldHolder(this, field, heldValue.defaultValue().argumentTypeRepresentingHolder(name), field.get(heldValue.defaultValue()), name);
            } catch (IllegalAccessException e) {
                return null;
            }
        }

        @Override
        public CompletableFuture<Suggestions> suggestInner(StringReader reader, SuggestionsBuilder builder) {
            List<String> fields = heldValue.defaultValue().fields();
            return SharedSuggestionProvider.suggest(fields, builder);
        }

        @Override
        public int postUpdate(CommandSourceStack commandSourceStack) {
            try {
                AtlasConfig config = this.heldValue.owner;
                config.saveConfig();
                commandSourceStack.getServer().getPlayerList().broadcastAll(ServerPlayNetworking.createS2CPacket(new AtlasCore.AtlasConfigPacket(true, config)));
                commandSourceStack.sendSuccess(() -> separatorLine(config.getFormattedName().copy(), true), true);
                if (restartRequired.restartRequiredOn(FabricLoader.getInstance().getEnvironmentType())) commandSourceStack.sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.holder_requires_restart.no_value", Component.translatable(getTranslationKey()))), true);
                else commandSourceStack.sendSuccess(() -> Component.literal("  » ").append(Component.translatable("text.config.update_holder.no_value", Component.translatable(getTranslationKey()))), true);
                commandSourceStack.sendSuccess(() -> separatorLine(null), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return 1;
        }

        @Override
        public void setValue(T newValue) {
            newValue.setOwnerHolder(this);
            super.setValue(newValue);
        }

        @Override
        public void setSynchedValue(T newValue) {
            newValue.setOwnerHolder(this);
            super.setSynchedValue(newValue);
        }
    }
    public static class EnumHolder<E extends Enum<E>> extends ConfigHolder<E, FriendlyByteBuf> {
        public final Class<E> clazz;
		public final Function<Enum, Component> names;
        private EnumHolder(ConfigValue<E> value, Class<E> clazz, Function<Enum, Component> names) {
            super(value, new StreamCodec<>() {
                @Override
                public void encode(FriendlyByteBuf object, E object2) {
                    object.writeEnum(object2);
                }

                @Override
                public @NotNull E decode(FriendlyByteBuf object) {
                    return object.readEnum(clazz);
                }
            }, (writer, e) -> {
                try {
                    writer.value(e.name().toLowerCase());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            this.clazz = clazz;
			this.names = names;
        }

		public void setValueAndResetManaged(String name) {
            setValueAndResetManaged(Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)));
        }

        public void setSynchedValue(String name) {
            setSynchedValue(Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)));
        }

        @Override
        public Component getValueAsComponent() {
            return names.apply(get());
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			return new EnumListEntry<>(Component.translatable(getTranslationKey()), clazz, get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, names, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
		}

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            for (E e : heldValue.possibleValues) {
                if (e.name().toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(e.name().toLowerCase());
                }
            }
            return builder.buildFuture();
        }

        @Override
        public <S> E parse(StringReader stringReader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            String name = stringReader.readString();
            for (E e : heldValue.possibleValues) {
                if (e.name().toLowerCase().equals(name.toLowerCase())) {
                    parsedValue = e;
                    return e;
                }
            }
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().create("a valid enum input");
        }
    }
    public static class StringHolder extends ConfigHolder<String, ByteBuf> {
        private StringHolder(ConfigValue<String> value) {
            super(value, ByteBufCodecs.STRING_UTF8, (writer, s) -> {
                try {
                    writer.value(s);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        @Override
        public Component getValueAsComponent() {
            return Component.literal(get());
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			return new StringListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
		}

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            if (heldValue.possibleValues != null) return SharedSuggestionProvider.suggest(heldValue.possibleValues, builder);
            return Suggestions.empty();
        }

        @Override
        public <S> String parse(StringReader stringReader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            parsedValue = stringReader.readString();
            return parsedValue;
        }
    }
    public static class BooleanHolder extends ConfigHolder<Boolean, ByteBuf> {
        private BooleanHolder(ConfigValue<Boolean> value) {
            super(value, ByteBufCodecs.BOOL, (writer, b) -> {
				try {
					writer.value(b);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
        }

        @Override
        public Component getValueAsComponent() {
            return get() ? Component.translatable("text.config.true") : Component.translatable("text.config.false");
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			return new BooleanListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
		}

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            if ("true".startsWith(builder.getRemainingLowerCase())) {
                builder.suggest("true");
            }
            if ("false".startsWith(builder.getRemainingLowerCase())) {
                builder.suggest("false");
            }
            return builder.buildFuture();
        }

        @Override
        public <S> Boolean parse(StringReader stringReader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            parsedValue = stringReader.readBoolean();
            return parsedValue;
        }
    }
    public static class IntegerHolder extends ConfigHolder<Integer, ByteBuf> {
		public final boolean isSlider;
        private IntegerHolder(ConfigValue<Integer> value, boolean isSlider) {
            super(value, ByteBufCodecs.VAR_INT, (writer, i) -> {
				try {
					writer.value(i);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
            this.isSlider = isSlider;
        }

        @Override
        public boolean isNotValid(Integer newValue) {
			if (heldValue.possibleValues == null)
				return super.isNotValid(newValue);
            boolean inRange = heldValue.isRange && newValue >= heldValue.possibleValues[0] && newValue <= heldValue.possibleValues[1];
            return super.isNotValid(newValue) && !inRange;
        }

        @Override
        public Component getValueAsComponent() {
            return Component.literal(String.valueOf(get()));
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			if (!heldValue.isRange || !isSlider)
				return new IntegerListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
			return new IntegerSliderEntry(Component.translatable(getTranslationKey()), heldValue.possibleValues[0], heldValue.possibleValues[1], get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
		}

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            if (heldValue.possibleValues != null && !heldValue.isRange) {
                for (Integer i : heldValue.possibleValues) {
                    if (i.toString().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(i.toString());
                    }
                }
                return builder.buildFuture();
            }
            return Suggestions.empty();
        }

        @Override
        public <S> Integer parse(StringReader reader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            final int start = reader.getCursor();
            final int result = reader.readInt();
            if (heldValue.possibleValues != null) {
                if (heldValue.isRange) {
                    if (result < heldValue.possibleValues[0]) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooLow().createWithContext(reader, result, heldValue.possibleValues[0]);
                    }
                    if (result > heldValue.possibleValues[1]) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.integerTooHigh().createWithContext(reader, result, heldValue.possibleValues[1]);
                    }
                } else {
                    if (Arrays.stream(heldValue.possibleValues).noneMatch(integer -> integer == result)) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, result);
                    }
                }
            }
            parsedValue = result;
            return result;
        }
    }
    public static class DoubleHolder extends ConfigHolder<Double, ByteBuf> {
        private DoubleHolder(ConfigValue<Double> value) {
            super(value, ByteBufCodecs.DOUBLE, (writer, d) -> {
				try {
					writer.value(d);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
        }

        @Override
        public boolean isNotValid(Double newValue) {
			if (heldValue.possibleValues == null)
				return super.isNotValid(newValue);
            boolean inRange = heldValue.isRange && newValue >= heldValue.possibleValues[0] && newValue <= heldValue.possibleValues[1];
            return super.isNotValid(newValue) && !inRange;
        }

        @Override
        public Component getValueAsComponent() {
            return Component.literal(String.valueOf(get()));
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			return new DoubleListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
		}

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            if (heldValue.possibleValues != null && !heldValue.isRange) {
                for (Double d : heldValue.possibleValues) {
                    if (d.toString().startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(d.toString());
                    }
                }
                return builder.buildFuture();
            }
            return Suggestions.empty();
        }

        @Override
        public <S> Double parse(StringReader reader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            final int start = reader.getCursor();
            final double result = reader.readDouble();
            if (heldValue.possibleValues != null) {
                if (heldValue.isRange) {
                    if (result < heldValue.possibleValues[0]) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooLow().createWithContext(reader, result, heldValue.possibleValues[0]);
                    }
                    if (result > heldValue.possibleValues[1]) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.doubleTooHigh().createWithContext(reader, result, heldValue.possibleValues[1]);
                    }
                } else {
                    if (Arrays.stream(heldValue.possibleValues).noneMatch(integer -> integer == result)) {
                        reader.setCursor(start);
                        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidDouble().createWithContext(reader, result);
                    }
                }
            }
            parsedValue = result;
            return result;
        }
	}
    public static class ColorHolder extends ConfigHolder<Integer, ByteBuf> {
        private boolean hasAlpha;

        private ColorHolder(ConfigValue<Integer> value, boolean alpha) {
            super(value, ByteBufCodecs.VAR_INT, (writer, d) -> {
                try {
                    String val = "#" + toColorHex(alpha, d);
                    writer.value(val);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            hasAlpha = alpha;
        }

        @Override
        public boolean isNotValid(Integer newValue) {
            String hex = Integer.toHexString(newValue);
            if (hex.length() > 8)
                return true;
            if (!hasAlpha && hex.length() > 6)
                return true;
            return super.isNotValid(newValue);
        }

        @Override
        public Component getValueAsComponent() {
            return Component.literal("#" + toColorHex(hasAlpha, get()));
        }

        public static String toColorHex(boolean hasAlpha, int val) {
            int i = 6;
            if (hasAlpha)
                i = 8;
            String toHex = Integer.toHexString(val);
            while (toHex.length() < i) {
                toHex = "0".concat(toHex);
            }
            return toHex;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public AbstractConfigListEntry<?> transformIntoConfigEntry() {
            ColorEntry entry = new ColorEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired.restartRequiredOn(EnvType.CLIENT));
            if (hasAlpha) entry.withAlpha();
            else entry.withoutAlpha();
            return entry;
        }

        @Override
        public <S> CompletableFuture<Suggestions> buildSuggestions(CommandContext<S> commandContext, SuggestionsBuilder builder) {
            return Suggestions.empty();
        }

        @Override
        public <S> Integer parse(StringReader reader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
            final String hex = reader.readString();
            stripHexStarter(hex);
            int result = (int) Long.parseLong(hex, 16);
            if (hex.length() > 8)
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, result);
            if (!hasAlpha && hex.length() > 6)
                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, result);
            parsedValue = result;
            return result;
        }
    }

    public void reset() {
        try {
            try (InputStream inputStream = getDefaultedConfig()) {
                Files.write(configFile.toPath(), inputStream.readAllBytes());
            }
        } catch (IOException e) {
            throw new ReportedException(new CrashReport("Failed to recreate config file for config " + name, e));
        }
        reload();
    }

	public void reloadFromDefault() {
		resetExtraHolders();
		isDefault = true;
		JsonObject configJsonObject = JsonParser.parseReader(new JsonReader(new InputStreamReader(getDefaultedConfig()))).getAsJsonObject();

        for (ObjectHolder<?> objectHolder : objectValues)
            if (configJsonObject.has(objectHolder.heldValue.name)) {
                objectHolder.setSynchedFromJSONObject(configJsonObject.getAsJsonObject(objectHolder.heldValue.name));
            }
		for (EnumHolder<?> enumHolder : enumValues)
			if (configJsonObject.has(enumHolder.heldValue.name)) {
				enumHolder.setSynchedValue(getString(configJsonObject, enumHolder.heldValue.name));
			}
		for (StringHolder stringHolder : stringValues)
			if (configJsonObject.has(stringHolder.heldValue.name)) {
				stringHolder.setSynchedValue(getString(configJsonObject, stringHolder.heldValue.name));
			}
		for (BooleanHolder booleanHolder : booleanValues)
			if (configJsonObject.has(booleanHolder.heldValue.name)) {
				booleanHolder.setSynchedValue(getBoolean(configJsonObject, booleanHolder.heldValue.name));
			}
		for (IntegerHolder integerHolder : integerValues)
			if (configJsonObject.has(integerHolder.heldValue.name)) {
				integerHolder.setSynchedValue(getInt(configJsonObject, integerHolder.heldValue.name));
			}
		for (DoubleHolder doubleHolder : doubleValues)
			if (configJsonObject.has(doubleHolder.heldValue.name)) {
				doubleHolder.setSynchedValue(getDouble(configJsonObject, doubleHolder.heldValue.name));
			}
        for (ColorHolder colorHolder : colorValues)
            if (configJsonObject.has(colorHolder.heldValue.name)) {
                colorHolder.setSynchedValue(getColor(configJsonObject, colorHolder.heldValue.name, colorHolder));
            }
		loadExtra(configJsonObject);
	}

    @Environment(EnvType.CLIENT)
	public static void handleExtraSyncStatic(AtlasCore.AtlasConfigPacket packet, ClientPlayNetworking.Context context) {
        if (!packet.forCommand()) {
            MutableComponent disconnectReason = Component.translatable("text.config.mismatch");
            AtomicBoolean isMismatched = new AtomicBoolean(false);
            configs.values().forEach(config -> {
                ClientPlayNetworking.send(new AtlasCore.ClientInformPacket(config));
                List<ConfigHolder<?, ? extends ByteBuf>> restartRequiredHolders = new ArrayList<>();
                config.valueNameToConfigHolderMap.values().forEach(configHolder -> {
                    if (configHolder.restartRequired.restartRequiredOn(EnvType.CLIENT) && configHolder.wasUpdated())
                        restartRequiredHolders.add(configHolder);
                });
                if (!restartRequiredHolders.isEmpty()) {
                    isMismatched.set(true);
                    restartRequiredHolders.forEach(configHolder -> configHolder.setToSynchedValue());
                    try {
                        config.saveConfig();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Consumer<MutableComponent> appender = component -> disconnectReason.append(component.withStyle(component.getStyle().withStrikethrough(false)));
                    Consumer<MutableComponent> appenderWithLineBreak = component -> disconnectReason.append(component.append(Component.literal("\n")).withStyle(component.getStyle().withStrikethrough(false)));
                    appenderWithLineBreak.accept(separatorLine(config.getFormattedName().copy(), true));
                    Consumer<ConfigHolder<?, ? extends ByteBuf>> lister = (configHolder) -> {
                        appender.accept(Component.literal("  » ").append(Component.translatable("text.config.holder.sync_mismatch", Component.translatable(configHolder.getTranslationKey()).withStyle(ChatFormatting.YELLOW))));
                        if (configHolder instanceof AtlasConfig.ExtendedHolder extendedHolder) {
                            AtomicReference<MutableComponent> entries = new AtomicReference<>(Component.literal("\n"));
                            appender.accept(entries.get());
                            configHolder.setToPreviousValue();
                            extendedHolder.fulfilListing((component) -> entries.set(entries.get().append(Component.literal("    » | ").append(component).append(Component.literal("\n")))));
                            appenderWithLineBreak.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_client_value", entries.get())));
                            configHolder.setToPreviousValue();
                            appenderWithLineBreak.accept(separatorLine(null));
                            entries.set(Component.literal("\n"));
                            extendedHolder.fulfilListing((component) -> entries.set(entries.get().append(Component.literal("    » | ").append(component).append(Component.literal("\n")))));
                            appenderWithLineBreak.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_server_value", entries.get())));
                            appenderWithLineBreak.accept(separatorLine(null));
                        } else {
                            appender.accept(Component.translatable("text.config.holder.sync_client_value", configHolder.getValueAsComponent()));
                            appender.accept(Component.literal(" / "));
                            appender.accept(Component.translatable("text.config.holder.sync_server_value", configHolder.getValueAsComponent()));
                        }
                    };
                    restartRequiredHolders.forEach(lister);
                    appenderWithLineBreak.accept(separatorLine(null));
                }
            });
            if (isMismatched.get()) {
                context.responseSender().disconnect(disconnectReason);
                return;
            }
        } else {
            AtomicBoolean isMismatched = new AtomicBoolean(false);
            configs.values().forEach(config -> {
                ClientPlayNetworking.send(new AtlasCore.ClientInformPacket(config));
                List<ConfigHolder<?, ? extends ByteBuf>> restartRequiredHolders = new ArrayList<>();
                config.valueNameToConfigHolderMap.values().forEach(configHolder -> {
                    if (configHolder.restartRequired.restartRequiredOn(EnvType.CLIENT) && configHolder.wasUpdated())
                        restartRequiredHolders.add(configHolder);
                });
                if (!restartRequiredHolders.isEmpty()) {
                    isMismatched.set(true);
                    restartRequiredHolders.forEach(configHolder -> configHolder.setToSynchedValue());
                    try {
                        config.saveConfig();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    restartRequiredHolders.forEach(configHolder -> configHolder.setToPreviousValue());
                    config.valueNameToConfigHolderMap.values().forEach(configHolder -> configHolder.setToPreviousValue());
                }
            });
            if (isMismatched.get()) {
                context.client().getChatListener().handleSystemMessage(Component.translatable("text.config.command.mismatch"), false);
            }
        }
        packet.config().handleExtraSync(packet, context);
    }
    @Environment(EnvType.CLIENT)
    public abstract void handleExtraSync(AtlasCore.AtlasConfigPacket packet, ClientPlayNetworking.Context context);
    public abstract void handleConfigInformation(AtlasCore.ClientInformPacket packet, ServerPlayer player, PacketSender sender);
	@Environment(EnvType.CLIENT)
	public abstract Screen createScreen(Screen prevScreen);
	@Environment(EnvType.CLIENT)
	public boolean hasScreen() {
		return true;
	}

	public record Category(AtlasConfig config, String name, List<ConfigHolder<?, ? extends ByteBuf>> members) {
		public String translationKey() {
			return "text.config." + config.name.getPath() + ".category." + name;
		}
		public void addMember(ConfigHolder<?, ? extends ByteBuf> member) {
			members.add(member);
		}

		@Environment(EnvType.CLIENT)
		public List<AbstractConfigListEntry<?>> membersAsCloth() {
			List<AbstractConfigListEntry<?>> transformed = new ArrayList<>();
			members.forEach(configHolder -> {
				AbstractConfigListEntry<?> entry = configHolder.transformIntoConfigEntry();
				entry.setEditable(!configHolder.serverManaged);
				transformed.add(entry);
			});
			return transformed;
		}
	}
}
