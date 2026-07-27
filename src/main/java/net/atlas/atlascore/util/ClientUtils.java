package net.atlas.atlascore.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;

public class ClientUtils {
    public static Screen getScreen(Minecraft client) {
        //? >=26.2 {
        return client.gui.screen();
        //?}
        //? <26.2 {
        /*return client.screen;
        *///?}
    }
    public static void setScreen(Minecraft client, Screen screen) {
        //? >=26.2 {
        client.setScreenAndShow(screen);
        //?}
        //? <26.2 {
        /*client.setScreen(screen);
        *///?}
    }
    public static WidgetSprites buildNoFocusedDisabled(Identifier base) {
        return new WidgetSprites(base, base.withSuffix("_disabled"), base.withSuffix("_highlighted"));
    }
    public static WidgetSprites buildFull(Identifier base) {
        return new WidgetSprites(base, base.withSuffix("_disabled"), base.withSuffix("_highlighted"), base.withSuffix("_highlighted_disabled"));
    }
}
