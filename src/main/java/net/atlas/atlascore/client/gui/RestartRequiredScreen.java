package net.atlas.atlascore.client.gui;

import net.atlas.atlascore.util.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class RestartRequiredScreen extends ConfirmScreen {
    public RestartRequiredScreen(Screen parent) {
        super((confirmed) -> {
            if (confirmed) Minecraft.getInstance().stop();
            else ClientUtils.setScreen(Minecraft.getInstance(), parent);
        }, Component.translatable("text.config.command.mismatch.0"), Component.translatable("text.config.command.mismatch.1"), Component.translatable("text.config.leave_game"), Component.translatable("text.config.continue_anyways"));
    }
}