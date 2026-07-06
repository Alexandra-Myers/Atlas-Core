package net.atlas.atlascore.neoforge;

//? neoforge {
/*import net.atlas.atlascore.AtlasCore;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;

@Mod(value = AtlasCore.MOD_ID)
@EventBusSubscriber(modid = AtlasCore.MOD_ID)
public class AtlasCoreNeoForge {
    public AtlasCoreNeoForge(ModContainer container) {
        AtlasCore.init();
    }
    @SubscribeEvent
    static void onRegisterConfigurationTasks(RegisterConfigurationTasksEvent event) {
        if (ServerConfigurationNetworking.canSend((ServerConfigurationPacketListenerImpl) event.getListener(), AtlasCore.ClientboundModListRetrievalPacket.TYPE)) event.register(new AtlasCore.ClientModRetrievalTask());
    }
}
*///?}