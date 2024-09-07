package net.atlas.atlascore.util;

import java.util.Objects;
import java.util.function.Predicate;

public record ContextCondition(String name, Predicate<Context> condition) implements Predicate<Context> {
    public static final ContextCondition GENERIC = new ContextCondition("generic", context -> true);
    public boolean isGeneric() {
        return equals(GENERIC);
    }

    public boolean test(Context context) {
        return condition.test(context);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextCondition that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
