package net.atlas.atlascore.util;

import net.atlas.atlascore.backport.StreamCodec;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static cpw.mods.modlauncher.api.LamdbaExceptionUtils.uncheck;

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

    public static Collection<ModRepresentation> mapFromModContainers(Collection<IModInfo> mods, String[] ownerIds) {
        return mods.stream().map(modContainer -> {
            String[] concat = Arrays.copyOf(ownerIds, ownerIds.length + 1);
            concat[concat.length - 1] = modContainer.getModId();
            Collection<IModInfo> includedMods = modContainer.getOwningFile().getMods().stream().filter(extras -> Arrays.stream(concat).noneMatch(s -> s.equals(extras.getModId()))).toList();
            if (includedMods.isEmpty()) {
                return new ModRepresentation(modContainer.getDisplayName(), modContainer.getModId(), Collections.emptyList(), uncheck(() -> Version.parse(modContainer.getVersion().toString())));
            } else return new ModRepresentation(modContainer.getDisplayName(), modContainer.getModId(), mapFromModContainers(includedMods, concat), uncheck(() -> Version.parse(modContainer.getVersion().toString())));
        }).toList();
    }

    public void list(Consumer<String> stringConsumer, Function<ModRepresentation, String> name, String prefix) {
        stringConsumer.accept(prefix + name.apply(this) + ": " + version());
        if (!provided.isEmpty()) provided.stream().sorted(Comparator.comparing(name)).forEach(modRepresentation -> modRepresentation.list(string -> stringConsumer.accept("  " + string), name, prefix));
    }
}
