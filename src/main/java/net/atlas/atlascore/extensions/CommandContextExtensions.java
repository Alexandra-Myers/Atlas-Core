package net.atlas.atlascore.extensions;

import java.util.List;

public interface CommandContextExtensions {
    <V> List<V> getArguments(final Class<V> clazz);

    boolean hasArgument(String name);
}
