package net.atlas.atlascore.fabric;

//? fabric {
import net.atlas.atlascore.AtlasCore;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;

public class AtlasCoreFabric implements ModInitializer {
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        AtlasCore.init();
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, AtlasCore.ClientboundModListRetrievalPacket.TYPE))
                handler.addTask(new AtlasCore.ClientModRetrievalTask());
        });
    }
}
//?}