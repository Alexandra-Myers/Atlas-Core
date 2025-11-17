package net.atlas.atlascore.client;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasConfigScreen;
import net.atlas.atlascore.util.ModRepresentation;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@Mod.EventBusSubscriber(modid = AtlasCore.MOD_ID, value = Dist.CLIENT)
public class AtlasCoreClient {
    public static void load(FMLJavaModLoadingContext context) {
        ClientPlayNetworking.registerGlobalReceiver(AtlasCore.AtlasConfigPacket.TYPE, AtlasConfig::handleExtraSyncStatic);
        ClientPlayConnectionEvents.JOIN.register((packet, sender, client) -> sender.sendPacket(new AtlasCore.ServerboundClientModPacket(ModRepresentation.mapFromModContainers(FabricLoader.getInstance().getAllMods()))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((resourceLocation, atlasConfig) -> atlasConfig.reload()));
        context.getContainer().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> new AtlasConfigScreen(screen, minecraft.options, Component.translatable("title.atlas_config.name"))));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ModList.get().forEachModInOrder(modContainer -> {
            if (!AtlasConfig.menus.containsKey(modContainer.getModId())) return;
            AtlasConfig config = AtlasConfig.menus.get(modContainer.getModId());
            modContainer.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, screen) -> ScreenBuilder.buildAtlasConfig(screen, config)));
        });
    }
}
