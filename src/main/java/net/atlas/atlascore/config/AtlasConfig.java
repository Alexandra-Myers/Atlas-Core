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
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.atlas.atlascore.util.ComponentUtils.separatorLine;

@SuppressWarnings("ALL")
public abstract class AtlasConfig {
    public final ResourceLocation name;
	public boolean isDefault;
    public final Map<String, ConfigHolder<?, ? extends ByteBuf>> valueNameToConfigHolderMap = Maps.newHashMap();
	List<ConfigHolder<?, ? extends ByteBuf>> holders;
	public final List<Category> categories;
    public static final Map<ResourceLocation, AtlasConfig> configs = Maps.newHashMap();
	public static final Map<String, AtlasConfig> menus = Maps.newHashMap();
    public static boolean alreadyChecked = false;
    final Path configFolderPath;
    File configFile;
    JsonObject configJsonObject;
    List<ObjectHolder<?>> objectValues;
    List<EnumHolder<?>> enumValues;
    List<StringHolder> stringValues;
    List<BooleanHolder> booleanValues;
    List<IntegerHolder> integerValues;
    List<DoubleHolder> doubleValues;
    List<ColorHolder> colorValues;

    public AtlasConfig(ResourceLocation name) {
		this.name = name;
        objectValues = new ArrayList<>();
        enumValues = new ArrayList<>();
        stringValues = new ArrayList<>();
        booleanValues = new ArrayList<>();
        integerValues = new ArrayList<>();
        doubleValues = new ArrayList<>();
        colorValues = new ArrayList<>();
		holders = new ArrayList<>();
		categories = createCategories();
        defineConfigHolders();
        configFolderPath = Path.of(FabricLoader.getInstance().getConfigDir().getFileName().getFileName() + "/" + name.getNamespace());
        if (!Files.exists(configFolderPath))
            try {
                Files.createDirectory(configFolderPath);
            } catch (IOException e) {
                throw new ReportedException(new CrashReport("Failed to create config directory for config " + name, e));
            }

        load();
        configs.put(name, this);
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
	public void reload() {
		resetExtraHolders();
		load();
	}
    public final void load() {
		isDefault = false;
        configFile = new File(configFolderPath.toAbsolutePath() + "/" + name.getPath() + ".json");
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
        return createObject(name, defaultInstance, clazz, serialisation, streamCodec, true);
    }
    public <T extends ConfigRepresentable> ObjectHolder<T> createObject(String name, T defaultInstance, Class<T> clazz, JavaToJSONSerialisation<T> serialisation, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec, boolean expandByDefault) {
        ObjectHolder<T> objectHolder = new ObjectHolder<>(new ConfigValue<>(defaultInstance, null, false, name, this), clazz, serialisation, streamCodec, expandByDefault);
        defaultInstance.setOwnerHolder(objectHolder);
        objectValues.add(objectHolder);
        holders.add(objectHolder);
        return objectHolder;
    }
    public final <E extends Enum<E>> EnumHolder<E> createEnum(String name, E defaultVal, Class<E> clazz, E[] values, Function<Enum, Component> names) {
        EnumHolder<E> enumHolder = new EnumHolder<>(new ConfigValue<>(defaultVal, values, false, name, this), clazz, names);
        enumValues.add(enumHolder);
		holders.add(enumHolder);
        return enumHolder;
    }
    public StringHolder createStringRange(String name, String defaultVal, String... values) {
        StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, values, false, name, this));
        stringValues.add(stringHolder);
		holders.add(stringHolder);
        return stringHolder;
    }
	public StringHolder createString(String name, String defaultVal) {
		StringHolder stringHolder = new StringHolder(new ConfigValue<>(defaultVal, null, false, name, this));
		stringValues.add(stringHolder);
		holders.add(stringHolder);
		return stringHolder;
	}
    public BooleanHolder createBoolean(String name, boolean defaultVal) {
        BooleanHolder booleanHolder = new BooleanHolder(new ConfigValue<>(defaultVal, new Boolean[]{false, true}, false, name, this));
        booleanValues.add(booleanHolder);
		holders.add(booleanHolder);
        return booleanHolder;
    }
    public ColorHolder createColor(String name, Integer defaultVal, boolean alpha) {
        ColorHolder colorHolder = new ColorHolder(new ConfigValue<>(defaultVal, null, false, name, this), alpha);
        colorValues.add(colorHolder);
        holders.add(colorHolder);
        return colorHolder;
    }
	public IntegerHolder createIntegerUnbound(String name, Integer defaultVal) {
		IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, null, false, name, this), false);
		integerValues.add(integerHolder);
		holders.add(integerHolder);
		return integerHolder;
	}
    public IntegerHolder createInRestrictedValues(String name, Integer defaultVal, Integer... values) {
        IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, values, false, name, this), false);
        integerValues.add(integerHolder);
		holders.add(integerHolder);
        return integerHolder;
    }
    public IntegerHolder createInRange(String name, int defaultVal, int min, int max, boolean isSlider) {
        Integer[] range = new Integer[]{min, max};
        IntegerHolder integerHolder = new IntegerHolder(new ConfigValue<>(defaultVal, range, true, name, this), isSlider);
        integerValues.add(integerHolder);
		holders.add(integerHolder);
        return integerHolder;
    }
	public DoubleHolder createDoubleUnbound(String name, Double defaultVal) {
		DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, null, false, name, this));
		doubleValues.add(doubleHolder);
		holders.add(doubleHolder);
		return doubleHolder;
	}
    public DoubleHolder createInRestrictedValues(String name, Double defaultVal, Double... values) {
        DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, values, false, name, this));
        doubleValues.add(doubleHolder);
		holders.add(doubleHolder);
        return doubleHolder;
    }
    public DoubleHolder createInRange(String name, double defaultVal, double min, double max) {
        Double[] range = new Double[]{min, max};
        DoubleHolder doubleHolder = new DoubleHolder(new ConfigValue<>(defaultVal, range, true, name, this));
        doubleValues.add(doubleHolder);
		holders.add(doubleHolder);
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

	public record ConfigValue<T>(T defaultValue, T[] possibleValues, boolean isRange, String name, AtlasConfig owner) {
        public void emitChanged(T newValue) {
            owner.alertChange(this, newValue);
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
        private T value;
        private T prevValue = null;
        protected T parsedValue = null;
        public final ConfigValue<T> heldValue;
        public final StreamCodec<B, T> codec;
		public final BiConsumer<JsonWriter, T> update;
		public boolean restartRequired = false;
		public boolean serverManaged = false;
        public boolean wasUpdated = false;
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
            return value;
        }
        public boolean wasUpdated() {
            return wasUpdated;
        }
		public void writeToJSONFile(JsonWriter writer) throws IOException {
			writer.name(heldValue.name);
			update.accept(writer, value);
		}
        public void writeToBuf(B buf) {
            codec.encode(buf, value);
        }
        public void readFromBuf(B buf) {
            T newValue = codec.decode(buf);
            if (isNotValid(newValue))
                return;
            if (newValue == value)
                return;
            heldValue.emitChanged(newValue);
            prevValue = value;
            value = newValue;
			serverManaged = true;
            wasUpdated = true;
        }
        public boolean isNotValid(T newValue) {
            return !heldValue.isValid(newValue);
        }
		public void setValueAndResetManaged(T newValue) {
			setValue(newValue);
			serverManaged = false;
            wasUpdated = false;
		}
        public void setValue(T newValue) {
            if (isNotValid(newValue))
                return;
            heldValue.emitChanged(newValue);
            prevValue = value;
            value = newValue;
        }

        @Override
        public void resetValue() {
            setValue(heldValue.defaultValue());
        }

        public void tieToCategory(Category category) {
			category.addMember(this);
		}
		public void setRestartRequired(boolean restartRequired) {
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
            if (parsedValue == null || isNotValid(parsedValue))
                return;
            heldValue.emitChanged(parsedValue);
            prevValue = value;
            value = parsedValue;
            parsedValue = null;
        }

        public void setToPreviousValue(boolean serverManaged) {
            if (prevValue != null) setValue(prevValue);
            this.serverManaged = serverManaged;
        }

        public T getPreviousValue() {
            return prevValue;
        }

        public Component getPreviousValueAsComponent() {
            setToPreviousValue(false);
            Component ret = getValueAsComponent();
            setToPreviousValue(false);
            return ret;
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
    }
    public static interface ExtendedHolder {
        Component getInnerTranslation(String name);
        Component getInnerValue(String name);
        void listInner(String name, Consumer<Component> input);
        void fulfilListing(Consumer<Component> input);
        ConfigHolderLike<?, ? extends ByteBuf> findInner(StringReader reader) throws CommandSyntaxException;
        ConfigHolderLike<?, ? extends ByteBuf> retrieveInner(String name) throws CommandSyntaxException;
        CompletableFuture<Suggestions> suggestInner(StringReader reader, SuggestionsBuilder builder);
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
            this.clazz = clazz;
            this.serialisation = serialisation;
            this.expandByDefault = expandByDefault;
        }

        public void setFromJSONObject(JsonObject object) {
            setValue(serialisation.decoder().apply(this, object));
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
            List<String> fields = heldValue.defaultValue().fields();
            for (String s : fields) {
                if (s.toLowerCase().startsWith(builder.getRemainingLowerCase())) {
                    builder.suggest(s.toLowerCase());
                }
            }
            return builder.buildFuture();
        }

        @Override
        public <S> T parse(StringReader stringReader, S source, CommandContext<S> commandContext) throws CommandSyntaxException {
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
        public ConfigHolderLike<?, ? extends ByteBuf> findInner(StringReader reader) throws CommandSyntaxException {
            List<String> fields = heldValue.defaultValue().fields();
            String name = ConfigHolderArgument.readHolderName(reader);
            if (!fields.contains(name))
                throw ConfigHolderArgument.ERROR_MALFORMED_HOLDER.createWithContext(reader, name);
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
        public void resetValue() {
            T val = heldValue.defaultValue();
            val.setOwnerHolder(this);
            setValue(val);
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
			setValue(name);
			serverManaged = false;
		}

		public void setValue(String name) {
            setValue(Enum.valueOf(clazz, name.toUpperCase(Locale.ROOT)));
        }

        @Override
        public Component getValueAsComponent() {
            return names.apply(get());
        }

        @Override
		@Environment(EnvType.CLIENT)
		public AbstractConfigListEntry<?> transformIntoConfigEntry() {
			return new EnumListEntry<>(Component.translatable(getTranslationKey()), clazz, get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, names, tooltip, restartRequired);
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
			return new StringListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
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
			return new BooleanListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
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
				return new IntegerListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
			return new IntegerSliderEntry(Component.translatable(getTranslationKey()), heldValue.possibleValues[0], heldValue.possibleValues[1], get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
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
			return new DoubleListEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
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
            ColorEntry entry = new ColorEntry(Component.translatable(getTranslationKey()), get(), Component.translatable(getTranslationResetKey()), () -> heldValue.defaultValue, this::setValue, tooltip, restartRequired);
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
                objectHolder.setFromJSONObject(configJsonObject.getAsJsonObject(objectHolder.heldValue.name));
                objectHolder.serverManaged = true;
            }
		for (EnumHolder<?> enumHolder : enumValues)
			if (configJsonObject.has(enumHolder.heldValue.name)) {
				enumHolder.setValue(getString(configJsonObject, enumHolder.heldValue.name));
				enumHolder.serverManaged = true;
			}
		for (StringHolder stringHolder : stringValues)
			if (configJsonObject.has(stringHolder.heldValue.name)) {
				stringHolder.setValue(getString(configJsonObject, stringHolder.heldValue.name));
				stringHolder.serverManaged = true;
			}
		for (BooleanHolder booleanHolder : booleanValues)
			if (configJsonObject.has(booleanHolder.heldValue.name)) {
				booleanHolder.setValue(getBoolean(configJsonObject, booleanHolder.heldValue.name));
				booleanHolder.serverManaged = true;
			}
		for (IntegerHolder integerHolder : integerValues)
			if (configJsonObject.has(integerHolder.heldValue.name)) {
				integerHolder.setValue(getInt(configJsonObject, integerHolder.heldValue.name));
				integerHolder.serverManaged = true;
			}
		for (DoubleHolder doubleHolder : doubleValues)
			if (configJsonObject.has(doubleHolder.heldValue.name)) {
				doubleHolder.setValue(getDouble(configJsonObject, doubleHolder.heldValue.name));
				doubleHolder.serverManaged = true;
			}
        for (ColorHolder colorHolder : colorValues)
            if (configJsonObject.has(colorHolder.heldValue.name)) {
                colorHolder.setValue(getColor(configJsonObject, colorHolder.heldValue.name, colorHolder));
                colorHolder.serverManaged = true;
            }
		loadExtra(configJsonObject);
	}

	public static void handleExtraSyncStatic(AtlasCore.AtlasConfigPacket packet, LocalPlayer player, PacketSender sender) {
        if (!packet.forCommand() && !alreadyChecked) {
            MutableComponent disconnectReason = Component.translatable("text.config.mismatch");
            AtomicBoolean isMismatched = new AtomicBoolean(false);
            configs.values().forEach(config -> {
                List<ConfigHolder<?, ? extends ByteBuf>> restartRequiredHolders = new ArrayList<>();
                config.holders.forEach(configHolder -> {
                    if (configHolder.restartRequired && configHolder.wasUpdated)
                        restartRequiredHolders.add(configHolder);
                });
                if (!restartRequiredHolders.isEmpty()) {
                    isMismatched.set(true);
                    config.reload();
                    restartRequiredHolders.forEach(configHolder -> configHolder.setToPreviousValue(true));
                    try {
                        config.saveConfig();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    Consumer<MutableComponent> appender = component -> disconnectReason.append(component.append(Component.literal("\n")).withStyle(component.getStyle().withStrikethrough(false)));
                    appender.accept(separatorLine(config.getFormattedName(), true));
                    Consumer<ConfigHolder<?, ? extends ByteBuf>> lister = (configHolder) -> {
                        appender.accept(Component.literal("  » ").append(Component.translatable("text.config.holder.sync_restart_required", Component.translatable(configHolder.getTranslationKey()).withStyle(ChatFormatting.YELLOW))));
                        if (configHolder instanceof AtlasConfig.ExtendedHolder extendedHolder) {
                            AtomicReference<MutableComponent> entries = new AtomicReference<>(Component.literal("\n"));
                            configHolder.setToPreviousValue(false);
                            extendedHolder.fulfilListing((component) -> entries.set(entries.get().append(Component.literal("    » | ").append(component).append(Component.literal("\n")))));
                            appender.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_current_value", entries.get())));
                            configHolder.setToPreviousValue(true);
                            entries.set(Component.literal("\n"));
                            extendedHolder.fulfilListing((component) -> entries.set(entries.get().append(Component.literal("    » | ").append(component).append(Component.literal("\n")))));
                            appender.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_expected_value", entries.get())));
                            appender.accept(separatorLine(null));
                        } else {
                            appender.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_current_value", configHolder.getPreviousValueAsComponent())));
                            appender.accept(Component.literal("  » | ").append(Component.translatable("text.config.holder.sync_expected_value", configHolder.getValueAsComponent())));
                        }
                    };
                    restartRequiredHolders.forEach(lister);
                    appender.accept(separatorLine(null));
                }
            });
            if (isMismatched.get()) {
                sender.disconnect(disconnectReason);
                alreadyChecked = true;
                return;
            }
        }
        packet.config().handleExtraSync(packet, player, sender);
    }
    public abstract void handleExtraSync(AtlasCore.AtlasConfigPacket packet, LocalPlayer player, PacketSender sender);
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
