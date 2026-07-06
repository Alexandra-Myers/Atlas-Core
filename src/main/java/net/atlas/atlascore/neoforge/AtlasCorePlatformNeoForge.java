package net.atlas.atlascore.neoforge;

//? neoforge {
/*import net.atlas.atlascore.AtlasCorePlatform;
import net.atlas.atlascore.util.ModRepresentation;
import net.fabricmc.loader.api.Version;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforgespi.language.IModInfo;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public class AtlasCorePlatformNeoForge implements AtlasCorePlatform {
    public static Collection<ModRepresentation> mapFromModContainers(Collection<IModInfo> mods, String[] ownerIds) {
        return mods.stream().map(modContainer -> {
            String[] concat = Arrays.copyOf(ownerIds, ownerIds.length + 1);
            concat[concat.length - 1] = modContainer.getModId();
            Collection<IModInfo> includedMods = modContainer.getOwningFile().getMods().stream().filter(extras -> Arrays.stream(concat).noneMatch(s -> s.equals(extras.getModId()))).toList();
            if (includedMods.isEmpty()) {
                return new ModRepresentation(modContainer.getDisplayName(), modContainer.getModId(), Collections.emptyList(), parse(modContainer.getVersion().toString()));
            } else return new ModRepresentation(modContainer.getDisplayName(), modContainer.getModId(), mapFromModContainers(includedMods, concat), parse(modContainer.getVersion().toString()));
        }).toList();
    }

    public static Version parse(String version) {
        try {
            return Version.parse(version);
        } catch (Exception e) {
            return ModRepresentation.UNKNOWN;
        }
    }

    @Override
    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public Collection<ModRepresentation> mapFromModContainers() {
        return mapFromModContainers(ModList.get().getMods(), new String[0]);
    }
}
*///?}