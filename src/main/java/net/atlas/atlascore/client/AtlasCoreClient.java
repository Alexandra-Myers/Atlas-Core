package net.atlas.atlascore.client;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasConfigScreen;
import net.atlas.atlascore.util.ModRepresentation;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = AtlasCore.MOD_ID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = AtlasCore.MOD_ID, value = Dist.CLIENT)
public class AtlasCoreClient {
    public AtlasCoreClient(ModContainer container) {
        // Allows NeoForge to create a config screen for this mod's configs.
        // The config screen is accessed by going to the Mods screen > clicking on your mod > clicking on config.
        // Do not forget to add translations for your config options to the en_us.json file.
        ClientPlayNetworking.registerGlobalReceiver(AtlasCore.AtlasConfigPacket.TYPE, AtlasConfig::handleExtraSyncStatic);
        ClientConfigurationNetworking.registerGlobalReceiver(AtlasCore.ClientboundModListRetrievalPacket.TYPE, (packet, sender) -> sender.responseSender().sendPacket(ClientPlayNetworking.createC2SPacket(new AtlasCore.ServerboundClientModPacket(ModRepresentation.mapFromModContainers(FabricLoader.getInstance().getAllMods())))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((resourceLocation, atlasConfig) -> atlasConfig.reload()));
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, screen) -> new AtlasConfigScreen(screen, Minecraft.getInstance().options, Component.translatable("title.atlas_config.name")));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        for (ModContainer modContainer : ModList.get().getSortedMods()) {
            if (!AtlasConfig.menus.containsKey(modContainer.getModId())) continue;
            AtlasConfig config = AtlasConfig.menus.get(modContainer.getModId());
            modContainer.registerExtensionPoint(IConfigScreenFactory.class, (container, screen) -> ScreenBuilder.buildAtlasConfig(screen, config));
        }
    }
}
