package net.atlas.atlascore.backport;

@FunctionalInterface
public interface StreamDecoder<I, T> {
    T decode(I object);
}