package net.atlas.atlascore;

//? fabric {
import net.atlas.atlascore.fabric.AtlasCorePlatformFabric;
//?}
//? neoforge {
/*import net.atlas.atlascore.neoforge.AtlasCorePlatformNeoForge;
*///?}
import net.atlas.atlascore.util.ModRepresentation;

import java.nio.file.Path;
import java.util.Collection;

public interface AtlasCorePlatform {
    //? fabric {
    AtlasCorePlatform INSTANCE = new AtlasCorePlatformFabric();
    //?}
    //? neoforge {
    /*AtlasCorePlatform INSTANCE = new AtlasCorePlatformNeoForge();
    *///?}
    Path getConfigDir();
    Collection<ModRepresentation> mapFromModContainers();
}
