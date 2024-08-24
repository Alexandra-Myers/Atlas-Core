package net.atlas.atlascore.config;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.arguments.*;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import me.shedaniel.clothconfig2.gui.entries.DoubleListEntry;
import me.shedaniel.clothconfig2.gui.entries.IntegerListEntry;
import me.shedaniel.clothconfig2.gui.entries.StringListEntry;
import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.util.ConfigRepresentable;
import net.atlas.atlascore.util.JavaToJSONSerialisation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.Util;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AtlasCoreConfig extends AtlasConfig {
    public static class TestClass implements ConfigRepresentable<TestClass> {
        public static final StreamCodec<RegistryFriendlyByteBuf, TestClass> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf registryFriendlyByteBuf, TestClass testClass) {
                registryFriendlyByteBuf.writeResourceLocation(testClass.owner.heldValue.owner().name);
                registryFriendlyByteBuf.writeUtf(testClass.owner.heldValue.name());
                registryFriendlyByteBuf.writeUtf(testClass.innerString);
                registryFriendlyByteBuf.writeBoolean(testClass.innerBool);
                registryFriendlyByteBuf.writeVarInt(testClass.innerInt);
                registryFriendlyByteBuf.writeDouble(testClass.innerDouble);
            }

            @Override
            @SuppressWarnings("unchecked")
            public @NotNull TestClass decode(RegistryFriendlyByteBuf registryFriendlyByteBuf) {
                AtlasConfig config = AtlasConfig.configs.get(registryFriendlyByteBuf.readResourceLocation());
                return new TestClass((ConfigHolder<TestClass, RegistryFriendlyByteBuf>) config.valueNameToConfigHolderMap.get(registryFriendlyByteBuf.readUtf()), registryFriendlyByteBuf.readUtf(), registryFriendlyByteBuf.readBoolean(), registryFriendlyByteBuf.readVarInt(), registryFriendlyByteBuf.readDouble());
            }
        };
        public ConfigHolder<TestClass, RegistryFriendlyByteBuf> owner;
        public String innerString;
        public Boolean innerBool;
        public Integer innerInt;
        public Double innerDouble;
        public static final Map<String, Field> fields = Util.make(new HashMap<>(), hashMap -> {
            try {
                hashMap.put("innerString", TestClass.class.getDeclaredField("innerString"));
                hashMap.put("innerBool", TestClass.class.getDeclaredField("innerBool"));
                hashMap.put("innerInt", TestClass.class.getDeclaredField("innerInt"));
                hashMap.put("innerDouble", TestClass.class.getDeclaredField("innerDouble"));
            } catch (NoSuchFieldException ignored) {
            }
        });
        public static final BiFunction<TestClass, String, Component> convertFieldToComponent = (testClass, string) -> {
            try {
                return Component.translatable(testClass.owner.getTranslationKey() + "." + string).append(Component.literal(": ")).append(Component.literal(String.valueOf(testClass.fieldRepresentingHolder(string).get(testClass))));
            } catch (IllegalAccessException ignored) {

            }
            return Component.translatable(testClass.owner.getTranslationKey() + "." + string);
        };
        public static final BiFunction<TestClass, String, Component> convertFieldToNameComponent = (testClass, string) -> Component.translatable(testClass.owner.getTranslationKey() + "." + string);
        public static final BiFunction<TestClass, String, Component> convertFieldToValueComponent = (testClass, string) -> {
            try {
                return Component.literal(String.valueOf(testClass.fieldRepresentingHolder(string).get(testClass)));
            } catch (IllegalAccessException ignored) {

            }
            return Component.translatable(testClass.owner.getTranslationKey() + "." + string);
        };
        public static final BiFunction<ConfigHolder<TestClass, RegistryFriendlyByteBuf>, JsonObject, TestClass> decoder = (objectHolder, jsonObject) -> {
            String innerString = "bar";
            Boolean innerBool = true;
            Integer innerInt = 3;
            Double innerDouble = 7.0;
            if (jsonObject.has("innerString"))
                innerString = getString(jsonObject, "innerString");
            if (jsonObject.has("innerBool"))
                innerBool = getBoolean(jsonObject, "innerBool");
            if (jsonObject.has("innerInt"))
                innerInt = getInt(jsonObject, "innerInt");
            if (jsonObject.has("innerDouble"))
                innerDouble = getDouble(jsonObject, "innerDouble");
            return new TestClass(objectHolder, innerString, innerBool, innerInt, innerDouble);
        };
        public static final BiConsumer<JsonWriter, TestClass> encoder = (jsonWriter, testClass) -> fields.forEach((string, field) -> {
            try {
                jsonWriter.name(string);
                var value = field.get(testClass);
                switch (value) {
                    case String str -> jsonWriter.value(str);
                    case Boolean bool -> jsonWriter.value(bool);
                    case Integer i -> jsonWriter.value(i);
                    case Double d -> jsonWriter.value(d);
                    default -> throw new IllegalStateException("Unexpected value: " + value);
                }
            } catch (IOException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
        public Supplier<Component> resetTranslation = null;

        public TestClass(ConfigHolder<TestClass, RegistryFriendlyByteBuf> owner, String innerString, Boolean innerBool, Integer innerInt, Double innerDouble) {
            this.owner = owner;
            this.innerString = innerString;
            this.innerBool = innerBool;
            this.innerInt = innerInt;
            this.innerDouble = innerDouble;
        }

        @Override
        public void setOwnerHolder(ConfigHolder<TestClass, RegistryFriendlyByteBuf> configHolder) {
            owner = configHolder;
        }

        @Override
        public List<String> fields() {
            return fields.keySet().stream().toList();
        }

        @Override
        public Component getFieldValue(String name) {
            return convertFieldToValueComponent.apply(this, name);
        }

        @Override
        public Component getFieldName(String name) {
            return convertFieldToNameComponent.apply(this, name);
        }

        @Override
        public void listField(String name, Consumer<Component> input) {
            input.accept(convertFieldToComponent.apply(this, name));
        }

        @Override
        public void listFields(Consumer<Component> input) {
            fields.keySet().forEach(string -> input.accept(convertFieldToComponent.apply(this, string)));
        }

        @Override
        public Field fieldRepresentingHolder(String name) {
            return fields.get(name);
        }

        @Override
        public ArgumentType<?> argumentTypeRepresentingHolder(String name) {
            try {
                return switch (fields.get(name).get(this)) {
                    case String ignored -> StringArgumentType.greedyString();
                    case Boolean ignored -> BoolArgumentType.bool();
                    case Integer ignored -> IntegerArgumentType.integer();
                    case Double ignored -> DoubleArgumentType.doubleArg();
                    case null, default -> null;
                };
            } catch (IllegalAccessException ignored) {
            }
            return null;
        }

        @Override
        @Environment(EnvType.CLIENT)
        public List<AbstractConfigListEntry<?>> transformIntoConfigEntries() {
            if (resetTranslation == null)
                resetTranslation = () -> Component.translatable(owner.getTranslationResetKey());
            List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
            entries.add(new StringListEntry(convertFieldToNameComponent.apply(this, "innerString"), innerString, resetTranslation.get(), () -> "bar", string -> innerString = string, Optional::empty, false));
            entries.add(new BooleanListEntry(convertFieldToNameComponent.apply(this, "innerBool"), innerBool, resetTranslation.get(), () -> true, bool -> innerBool = bool, Optional::empty, false));
            entries.add(new IntegerListEntry(convertFieldToNameComponent.apply(this, "innerInt"), innerInt, resetTranslation.get(), () -> 3, integer -> innerInt = integer, Optional::empty, false));
            entries.add(new DoubleListEntry(convertFieldToNameComponent.apply(this, "innerDouble"), innerDouble, resetTranslation.get(), () -> 7.0, aDouble -> innerDouble = aDouble, Optional::empty, false));
            entries.forEach(entry -> entry.setEditable(!owner.serverManaged));
            return entries;
        }
    }
    public enum TestEnum {
        FOO,
        BAR
    }
    public ObjectHolder<TestClass> testObject;
    public EnumHolder<TestEnum> testEnum;
    public StringHolder testString;
    public BooleanHolder testBool;
    public IntegerHolder testInt;
    public DoubleHolder testDouble;
    public ColorHolder configNameDisplayColour;
    public ColorHolder grayFormattingColour;
    public BooleanHolder listClientModsOnJoin;
    private Category test;
    private Category formatting;
    private Category debug;
    public AtlasCoreConfig() {
        super(AtlasCore.id("atlas-core-config"));
        declareDefaultForMod(AtlasCore.MOD_ID);
    }

    @Override
    public void defineConfigHolders() {
        testObject = createObject("testObject", new TestClass(testObject, "bar", true, 3, 7.0), TestClass.class, new JavaToJSONSerialisation<>(TestClass.decoder, TestClass.encoder), TestClass.STREAM_CODEC);
        testObject.tieToCategory(test);
        testEnum = createEnum("testEnum", TestEnum.FOO, TestEnum.class, TestEnum.values(), e -> Component.translatable("text.config.atlas-core-config.option.testEnum." + e.name().toLowerCase(Locale.ROOT)));
        testEnum.tieToCategory(test);
        testString = createString("testString", "foo");
        testString.tieToCategory(test);
        testBool = createBoolean("testBool", true);
        testBool.tieToCategory(test);
        testInt = createInRestrictedValues("testInt", 1, 1, 3, 5, 7, 9);
        testInt.tieToCategory(test);
        testDouble = createDoubleUnbound("testDouble", 0.0);
        testDouble.tieToCategory(test);

        configNameDisplayColour = createColor("configNameDisplayColour", 57343, false);
        configNameDisplayColour.tieToCategory(formatting);
        grayFormattingColour = createColor("grayFormattingColour", 12502994, false);
        grayFormattingColour.tieToCategory(formatting);

        listClientModsOnJoin = createBoolean("listClientModsOnJoin", false);
        listClientModsOnJoin.tieToCategory(debug);
    }

    @Override
    public @NotNull List<Category> createCategories() {
        List<Category> categoryList = super.createCategories();
        test = new Category(this, "test_options", new ArrayList<>());
        formatting = new Category(this, "text_formatting", new ArrayList<>());
        debug = new Category(this, "debug_options", new ArrayList<>());
        categoryList.add(formatting);
        categoryList.add(debug);
        categoryList.add(test);
        return categoryList;
    }

    @Override
    public Component getFormattedName() {
        return Component.translatable("text.config." + name.getPath() + ".title").withStyle(Style.EMPTY.withColor(configNameDisplayColour.get()));
    }

    @Override
    public void resetExtraHolders() {

    }

    @Override
    public <T> void alertChange(ConfigValue<T> tConfigValue, T newValue) {

    }

    @Override
    public <T> void alertClientValue(ConfigValue<T> tConfigValue, T serverValue, T clientValue) {

    }

    @Override
    protected void loadExtra(JsonObject jsonObject) {

    }

    @Override
    protected InputStream getDefaultedConfig() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("atlas-core-config.json");
    }

    @Override
    public void saveExtra(JsonWriter jsonWriter, PrintWriter printWriter) {

    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handleExtraSync(AtlasCore.AtlasConfigPacket packet, ClientPlayNetworking.Context context) {

    }

    @Override
    public void handleConfigInformation(AtlasCore.ClientInformPacket packet, ServerPlayer player, PacketSender sender) {

    }

    @Override
    @Environment(EnvType.CLIENT)
    public Screen createScreen(Screen prevScreen) {
        return null;
    }
}
