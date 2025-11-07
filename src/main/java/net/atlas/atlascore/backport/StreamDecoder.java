package net.atlas.atlascore.backport;

@FunctionalInterface
interface StreamDecoder<I, T> {
    T decode(I object);
}