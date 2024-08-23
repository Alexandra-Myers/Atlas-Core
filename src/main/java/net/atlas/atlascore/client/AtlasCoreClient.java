package net.atlas.atlascore.client;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.config.AtlasConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AtlasCoreClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AtlasCore.AtlasConfigPacket.TYPE, (packet, context) -> AtlasConfig.handleExtraSyncStatic(packet, context.player(), context.responseSender()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((resourceLocation, atlasConfig) -> atlasConfig.reload()));
    }
}
