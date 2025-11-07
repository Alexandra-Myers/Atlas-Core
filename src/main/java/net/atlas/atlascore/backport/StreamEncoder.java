package net.atlas.atlascore.backport;

@FunctionalInterface
interface StreamEncoder<O, T> {
    void encode(O object, T object2);
}