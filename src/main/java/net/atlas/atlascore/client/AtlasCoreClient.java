package net.atlas.atlascore.client;

import net.atlas.atlascore.AtlasCore;
import net.atlas.atlascore.AtlasCorePlatform;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.util.CommonUtils;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class AtlasCoreClient {
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(AtlasCore.AtlasConfigPacket.TYPE, AtlasConfig::handleExtraSyncStatic);
        ClientConfigurationNetworking.registerGlobalReceiver(AtlasCore.ClientboundModListRetrievalPacket.TYPE, (packet, sender) -> sender.responseSender().sendPacket(CommonUtils.createServerboundPlayPacket(new AtlasCore.ServerboundClientModPacket(AtlasCorePlatform.INSTANCE.mapFromModContainers()))));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> AtlasConfig.configs.forEach((id, atlasConfig) -> atlasConfig.reload()));
    }
}
