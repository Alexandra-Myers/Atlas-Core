package net.atlas.atlascore.fabric.client;

import net.atlas.atlascore.client.AtlasCoreClient;
import net.fabricmc.api.ClientModInitializer;

public class AtlasCoreFabricClient implements ClientModInitializer {
    /**
     * Runs the mod initializer on the client environment.
     */
    @Override
    public void onInitializeClient() {
        AtlasCoreClient.init();
    }
}
