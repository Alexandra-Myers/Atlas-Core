package net.atlas.atlaslib.client;

import net.atlas.atlaslib.AtlasLib;
import net.atlas.atlaslib.config.AtlasConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AtlasLibClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(AtlasLib.AtlasConfigPacket.TYPE, (packet, context) -> packet.config().handleExtraSync(packet, context.player(), context.responseSender()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((resourceLocation, atlasConfig) -> atlasConfig.reload()));
    }
}
