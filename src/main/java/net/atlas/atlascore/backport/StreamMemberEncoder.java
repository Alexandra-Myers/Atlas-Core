package net.atlas.atlascore.backport;

@FunctionalInterface
interface StreamMemberEncoder<O, T> {
    void encode(T object, O object2);
}