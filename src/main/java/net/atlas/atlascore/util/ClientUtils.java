package net.atlas.atlascore.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

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
}
