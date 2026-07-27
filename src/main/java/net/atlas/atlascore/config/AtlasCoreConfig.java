package net.atlas.atlascore.config;

import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.atlas.atlascore.AtlasCore;
//? fabric {
import net.atlas.atlascore.config.fixer.ConfigFixer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
//?}
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
//? >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?}
//? <26.1 {
/*import net.minecraft.world.item.ItemStack;
*///?}
import net.minecraft.world.item.Items;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AtlasCoreConfig extends AtlasConfig {
    public record TestClass(String innerString, Boolean innerBool, Integer innerInt, Double innerDouble) {
            public static final Codec<TestClass> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(Codec.STRING.optionalFieldOf("inner_string", "bar").forGetter(TestClass::innerString),
                                    Codec.BOOL.optionalFieldOf("inner_bool", true).forGetter(TestClass::innerBool),
                                    Codec.INT.optionalFieldOf("inner_int", 3).forGetter(TestClass::innerInt),
                                    Codec.DOUBLE.optionalFieldOf("inner_double", 7.0).forGetter(TestClass::innerDouble))
                            .apply(instance, TestClass::new));

    }
    public enum TestEnum {
        FOO,
        BAR
    }
    //? >=26.1 {
    public TagHolder<ItemStackTemplate> testItem;
    //?}
    //? <26.1 {
    /*public TagHolder<ItemStack> testItem;
    *///?}
    public TagHolder<TestClass> testObject;
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
    public ConfigFixer createFixer() {
        return new ConfigFixer(this, List.of(Identifier.fromNamespaceAndPath("atlas-core", "atlas-core-config")));
    }

    @Override
    public void defineConfigHolders() {
        //? >=26.1 {
        testItem = createCodecBacked("testItem", new ItemStackTemplate(Items.APPLE, 18), ItemStackTemplate.CODEC);
        //?}
        //? <26.1 {
        /*testItem = createCodecBacked("testItem", new ItemStack(Items.APPLE, 18), ItemStack.STRICT_CODEC);
        *///?}
        testItem.tieToCategory(test);
        testObject = createCodecBacked("testObject", new TestClass("bar", true, 3, 7.0), TestClass.CODEC);
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
        return Component.translatableWithFallback("text.config." + name.getPath() + ".title", "Atlas Core").withStyle(Style.EMPTY.withColor(configNameDisplayColour.get()));
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
    //? fabric {
    @Environment(EnvType.CLIENT)
    //?}
    public void handleExtraSync(AtlasCore.AtlasConfigPacket packet, ClientPlayNetworking.Context context) {

    }

    @Override
    public void handleConfigInformation(AtlasCore.ClientInformPacket packet, ServerPlayer player, PacketSender sender) {

    }

    @Override
    //? fabric {
    @Environment(EnvType.CLIENT)
    //?}
    public Screen createScreen(Screen prevScreen) {
        return null;
    }
}
