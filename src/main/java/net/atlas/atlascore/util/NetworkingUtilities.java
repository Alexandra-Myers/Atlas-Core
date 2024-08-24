package net.atlas.atlascore.util;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NetworkingUtilities {
    public static Collection<ModRepresentation> modsFromNetwork(FriendlyByteBuf buf) {
        List<ModRepresentation> containers = new ArrayList<>();
        final int listSize = buf.readVarInt();
        for (int index = 0; index < listSize; index++) {
            containers.add(index, ModRepresentation.CODEC.decode(buf));
        }
        return Collections.unmodifiableList(containers);
    }
    public static void modsToNetwork(FriendlyByteBuf buf, Collection<ModRepresentation> modRepresentations) {
        buf.writeVarInt(modRepresentations.size());
        for (ModRepresentation modRepresentation : modRepresentations) {
            ModRepresentation.CODEC.encode(buf, modRepresentation);
        }
    }
}
