package net.atlas.atlascore.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.Collection;

public class ServerModsRetrievedEvent {
    /**
     * An event for when a server to handle a client's mod list.
     * Invoked upon the server receiving the mods from the client during the <b>CONFIGURATON</b> phase.
     *
     * <p> Packets sent during this phase should be registered during the configuration phase.
     */
    public static final Event<ServerModsRetrievedEvent.RetrieveMods> RETRIEVAL = EventFactory.createArrayBacked(ServerModsRetrievedEvent.RetrieveMods.class, callbacks -> (handler, sender, mods) -> {
        for (ServerModsRetrievedEvent.RetrieveMods callback : callbacks) {
            callback.onModsReceived(handler, sender, mods);
        }
    });

    @FunctionalInterface
    public interface RetrieveMods {
        void onModsReceived(ServerConfigurationPacketListenerImpl handler, PacketSender sender, Collection<ModRepresentation> mods);
    }
}
