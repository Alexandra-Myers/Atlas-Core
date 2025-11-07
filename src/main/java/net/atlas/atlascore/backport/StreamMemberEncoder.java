package net.atlas.atlascore.backport;

@FunctionalInterface
public interface StreamMemberEncoder<O, T> {
    void encode(T object, O object2);
}