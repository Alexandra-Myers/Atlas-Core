package net.atlas.atlascore.fabric;

//? fabric {
import net.atlas.atlascore.AtlasCorePlatform;
import net.atlas.atlascore.util.ModRepresentation;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class AtlasCorePlatformFabric implements AtlasCorePlatform {
    public static Collection<ModRepresentation> mapFromModContainers(Collection<ModContainer> mods, String[] ownerIds) {
        return mods.stream().map(modContainer -> {
            String[] concat = Arrays.copyOf(ownerIds, ownerIds.length + 1);
            concat[concat.length - 1] = modContainer.getMetadata().getId();
            Collection<ModContainer> includedMods = modContainer.getContainedMods().stream().filter(extras -> Arrays.stream(concat).noneMatch(s -> s.equals(extras.getMetadata().getId()))).toList();
            if (includedMods.isEmpty()) {
                return new ModRepresentation(modContainer.getMetadata().getName(), modContainer.getMetadata().getId(), Collections.emptyList(), modContainer.getMetadata().getVersion());
            } else return new ModRepresentation(modContainer.getMetadata().getName(), modContainer.getMetadata().getId(), mapFromModContainers(includedMods, concat), modContainer.getMetadata().getVersion());
        }).toList();
    }

    @Override
    public Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public Collection<ModRepresentation> mapFromModContainers() {
        return mapFromModContainers(FabricLoader.getInstance().getAllMods(), new String[0]);
    }
}
//?}