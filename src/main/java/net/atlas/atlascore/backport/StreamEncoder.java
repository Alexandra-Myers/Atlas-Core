package net.atlas.atlascore.backport;

@FunctionalInterface
public interface StreamEncoder<O, T> {
    void encode(O object, T object2);
}