package net.atlas.atlascore.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtils {
    /**
     * Produces a hash map from arrays where the size is the same, and the indices indicate which keys are aligned to which value.
     *
     * @param keys The keys for the map, arranged such that keys[i] maps to values[i]. Null keys at any index will skip that index.
     * @param values The values for the map, arranged such that values[i] is obtained from keys[i]. Null values at any index will skip that index.
     * @throws IndexOutOfBoundsException If the length of the keys and values are different, which should not happen if two arrays are sorted to match in indices.
     * @return A {@link HashMap} formed where each key is mapped to each value respective to the order they were in the arrays.
     * @param <K> The key type for this map.
     * @param <V> The value type for this map.
     */
    public static <K, V> Map<K, V> buildHashMapFromAlignedArrays(K[] keys, V[] values) {
        if (keys.length != values.length) throw new IndexOutOfBoundsException("Cannot map arrays of an unequal size!");
        Map<K, V> argumentTypeMap = new HashMap<>();
        for (int index = 0; index < keys.length; index++) {
            if (keys[index] == null || values[index] == null) continue;
            argumentTypeMap.put(keys[index], values[index]);
        }
        return argumentTypeMap;
    }
}
