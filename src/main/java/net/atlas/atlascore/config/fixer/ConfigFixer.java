package net.atlas.atlascore.config.fixer;

import com.google.gson.JsonObject;
import net.atlas.atlascore.config.AtlasConfig;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.io.IOException;

public class ConfigFixer {
    public final AtlasConfig owner;
    public ConfigFixer(AtlasConfig owner) {
        this.owner = owner;
    }
    public final void fix(JsonObject configJsonObject) throws IOException {
        MutableBoolean dirty = new MutableBoolean(false);
        for (AtlasConfig.Category category : this.owner.categories) {
            JsonObject categoryRoot;
            if (!configJsonObject.has(category.name())) {
                dirty.setTrue();
                categoryRoot = new JsonObject();
            } else categoryRoot = configJsonObject.getAsJsonObject(category.name());
            for (AtlasConfig.ConfigHolder<?> configHolder : category.members())
                configHolder.getFixer().fix(categoryRoot, configJsonObject, dirty);
            if (dirty.booleanValue()) configJsonObject.add(category.name(), categoryRoot);
        }
        for (AtlasConfig.ConfigHolder<?> configHolder : this.owner.getUncategorisedHolders())
            configHolder.getFixer().fix(null, configJsonObject, dirty);
        fixExtra(configJsonObject, dirty);
        if (dirty.booleanValue()) this.owner.saveFixedConfig(configJsonObject);
    }

    public void fixExtra(JsonObject configJsonObject, MutableBoolean dirty) {

    }
}