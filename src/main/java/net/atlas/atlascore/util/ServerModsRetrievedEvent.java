package net.atlas.atlascore.util;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.Collection;

public class ServerModsRetrievedEvent {
    /**
     * An event for when a server to handle a client's mod list.
     * Invoked upon the server receiving the mods from the client during the <b>CONFIGURATON</b> phase.
     *
     * <p> Packets sent during this phase should be registered during the configuration phase.
     */
    public static final Event<RetrieveMods> RETRIEVAL = EventFactory.createArrayBacked(RetrieveMods.class, callbacks -> (handler, sender, mods) -> {
        for (RetrieveMods callback : callbacks) {
            callback.onModsReceived(handler, sender, mods);
        }
    });

    @FunctionalInterface
    public interface RetrieveMods {
        void onModsReceived(ServerGamePacketListenerImpl handler, PacketSender sender, Collection<ModRepresentation> mods);
    }
}
