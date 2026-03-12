package net.atlas.atlascore.config.fixer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.atlas.atlascore.config.AtlasConfig;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConfigHolderFixer<T> {
    private final AtlasConfig.ConfigHolder<T> owner;
    private final List<String> pastCategories = new ArrayList<>();
    public ConfigHolderFixer(AtlasConfig.ConfigHolder<T> owner) {
        this.owner = owner;
    }
    final void fix(@Nullable JsonObject holderRootObject, @NotNull JsonObject configRootObject, MutableBoolean dirty) {
        JsonElement value = null;
        boolean readFromRoot = false;
        if (holderRootObject != null && holderRootObject.has(owner.getName())) value = holderRootObject.get(owner.getName());
        else if (configRootObject.has(owner.getName())) {
            value = configRootObject.get(owner.getName());
            readFromRoot = holderRootObject != null;
        }
        if (mustFix(value)) {
            value = fixData(value, holderRootObject, configRootObject);
            Objects.requireNonNullElse(holderRootObject, configRootObject).add(owner.getName(), value);
            dirty.setTrue();
        } else if (readFromRoot) {
            holderRootObject.add(owner.getName(), value);
            configRootObject.remove(owner.getName());
            dirty.setTrue();
        }
    }

    public JsonElement fixData(@Nullable JsonElement value, @Nullable JsonObject holderRootObject, @NotNull JsonObject configRootObject) {
        if (value == null) {
            for (String oldCategory : pastCategories) {
                if (!configRootObject.has(oldCategory)) continue;
                JsonObject categoryRoot = configRootObject.getAsJsonObject(oldCategory);
                if (!categoryRoot.has(this.owner.getName())) {
                    if (categoryRoot.isEmpty()) configRootObject.remove(oldCategory);
                    continue;
                }
                value = categoryRoot.get(this.owner.getName());
                categoryRoot.remove(this.owner.getName());
                if (categoryRoot.isEmpty()) configRootObject.remove(oldCategory);
            }
            if (value == null) { // Either no older categories, or simply not present in them
                Objects.requireNonNullElse(holderRootObject, configRootObject).add(this.owner.getName(), this.owner.encodeAsJSON().getOrThrow());
            }
        }
        return value;
    }

    public void addOldCategory(String categoryName) {
        this.pastCategories.add(categoryName);
    }

    private boolean mustFix(@Nullable JsonElement value) {
        return value == null;
    }
}
