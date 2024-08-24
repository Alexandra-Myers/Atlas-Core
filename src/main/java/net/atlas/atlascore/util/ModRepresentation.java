package net.atlas.atlascore.util;

import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

public record ModRepresentation(String name, String modID, Collection<ModRepresentation> provided, Version version) {
    public static final Version UNKNOWN;

    static {
        try {
            UNKNOWN = Version.parse("1.0.0-UNKNOWN");
        } catch (VersionParsingException e) {
            throw new RuntimeException(e);
        }
    }

    public static final StreamCodec<FriendlyByteBuf, ModRepresentation> CODEC = StreamCodec.of(ModRepresentation::writeToBuf, ModRepresentation::readFromBuf);

    public static ModRepresentation readFromBuf(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        String modID = buf.readUtf();
        int providedSize = buf.readVarInt();
        List<ModRepresentation> provided = new ArrayList<>();
        for (int i = 0; i < providedSize; i++) {
            provided.add(readFromBuf(buf));
        }
        Version version;
        try {
            version = Version.parse(buf.readUtf());
        } catch (VersionParsingException e) {
            version = UNKNOWN;
        }
        return new ModRepresentation(name, modID, Collections.unmodifiableList(provided), version);
    }

    public static void writeToBuf(FriendlyByteBuf buf, ModRepresentation modRepresentation) {
        buf.writeUtf(modRepresentation.name());
        buf.writeUtf(modRepresentation.modID());
        buf.writeVarInt(modRepresentation.provided().size());
        for (ModRepresentation str : modRepresentation.provided()) {
            writeToBuf(buf, str);
        }
        buf.writeUtf(modRepresentation.version().getFriendlyString());
    }

    public static Collection<ModRepresentation> mapFromModContainers(Collection<ModContainer> mods) {
        return mods.stream().map(modContainer -> {
            if (modContainer.getContainedMods().isEmpty()) {
                return new ModRepresentation(modContainer.getMetadata().getName(), modContainer.getMetadata().getId(), Collections.emptyList(), modContainer.getMetadata().getVersion());
            } else return new ModRepresentation(modContainer.getMetadata().getName(), modContainer.getMetadata().getId(), mapFromModContainers(modContainer.getContainedMods()), modContainer.getMetadata().getVersion());
        }).toList();
    }

    public void list(Consumer<String> stringConsumer, Function<ModRepresentation, String> name, String prefix) {
        stringConsumer.accept(prefix + name.apply(this) + ": " + version());
        if (!provided.isEmpty()) provided.stream().sorted(Comparator.comparing(name)).forEach(modRepresentation -> modRepresentation.list(string -> stringConsumer.accept("  " + string), name, prefix));
    }
}
