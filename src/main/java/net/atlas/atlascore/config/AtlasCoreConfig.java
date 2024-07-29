package net.atlas.atlascore.config;

import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.atlas.atlascore.AtlasCore;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AtlasCoreConfig extends AtlasConfig {
    public enum TestEnum {
        FOO,
        BAR
    }
    public EnumHolder<TestEnum> testEnum;
    public StringHolder testString;
    public BooleanHolder testBool;
    public IntegerHolder testInt;
    public DoubleHolder testDouble;
    public ColorHolder configNameDisplayColour;
    public ColorHolder grayFormattingColour;
    private Category test;
    private Category formatting;
    public AtlasCoreConfig() {
        super(AtlasCore.id("atlas-core-config"));
        declareDefaultForMod(AtlasCore.MOD_ID);
    }

    @Override
    public void defineConfigHolders() {
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
    }

    @Override
    public @NotNull List<Category> createCategories() {
        List<Category> categoryList = super.createCategories();
        test = new Category(this, "test_options", new ArrayList<>());
        formatting = new Category(this, "text_formatting", new ArrayList<>());
        categoryList.add(test);
        categoryList.add(formatting);
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
    protected void loadExtra(JsonObject jsonObject) {

    }

    @Override
    protected InputStream getDefaultedConfig() {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream("atlas-lib-config.json");
    }

    @Override
    public void saveExtra(JsonWriter jsonWriter, PrintWriter printWriter) {

    }

    @Override
    public void handleExtraSync(AtlasCore.AtlasConfigPacket packet, LocalPlayer player, PacketSender sender) {

    }

    @Override
    public Screen createScreen(Screen prevScreen) {
        return null;
    }
}
