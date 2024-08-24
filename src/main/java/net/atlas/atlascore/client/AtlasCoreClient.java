package net.atlas.atlascore.client;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.util.ModRepresentation;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;

public class AtlasCoreClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AtlasCore.AtlasConfigPacket.TYPE, AtlasConfig::handleExtraSyncStatic);
        ClientConfigurationNetworking.registerGlobalReceiver(AtlasCore.ClientboundModListRetrievalPacket.TYPE, (packet, sender) -> sender.responseSender().sendPacket(ClientPlayNetworking.createC2SPacket(new AtlasCore.ServerboundClientModPacket(ModRepresentation.mapFromModContainers(FabricLoader.getInstance().getAllMods())))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((resourceLocation, atlasConfig) -> atlasConfig.reload()));
    }
}
