package net.atlas.atlaslib.mixin;

import net.atlas.atlaslib.config.AtlasConfigScreen;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Supplier;

@Mixin(OptionsScreen.class)
public abstract class OptionsScreenMixin extends Screen {
    @Shadow
    protected abstract Button openScreenButton(Component component, Supplier<Screen> supplier);

	@Shadow
	@Final
	private Options options;

	protected OptionsScreenMixin(Component component) {
        super(component);
    }

    @Inject(method = "init",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    shift = At.Shift.AFTER, ordinal = 9),
            locals = LocalCapture.CAPTURE_FAILSOFT)
    public void injectCookeyModButton(CallbackInfo ci, LinearLayout linearLayout, LinearLayout linearLayout2, GridLayout gridLayout, GridLayout.RowHelper rowHelper) {
		rowHelper.addChild(this.openScreenButton(Component.translatable("options.atlas_config.button"), () -> {
			assert this.minecraft != null;
			return new AtlasConfigScreen(this.minecraft.screen, options, Component.translatable("title.atlas_config.name"));
		}));
    }
}
