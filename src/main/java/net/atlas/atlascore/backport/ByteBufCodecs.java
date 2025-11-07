package net.atlas.atlascore.backport;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.core.IdMap;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public interface ByteBufCodecs {
    int MAX_INITIAL_COLLECTION_SIZE = 65536;
    StreamCodec<ByteBuf, Boolean> BOOL = new StreamCodec<>() {
        public Boolean decode(ByteBuf byteBuf) {
            return byteBuf.readBoolean();
        }

        public void encode(ByteBuf byteBuf, Boolean boolean_) {
            byteBuf.writeBoolean(boolean_);
        }
    };
    StreamCodec<ByteBuf, Byte> BYTE = new StreamCodec<>() {
        public Byte decode(ByteBuf byteBuf) {
            return byteBuf.readByte();
        }

        public void encode(ByteBuf byteBuf, Byte byte_) {
            byteBuf.writeByte(byte_);
        }
    };
    StreamCodec<ByteBuf, Short> SHORT = new StreamCodec<>() {
        public Short decode(ByteBuf byteBuf) {
            return byteBuf.readShort();
        }

        public void encode(ByteBuf byteBuf, Short short_) {
            byteBuf.writeShort(short_);
        }
    };
    StreamCodec<ByteBuf, Integer> UNSIGNED_SHORT = new StreamCodec<>() {
        public Integer decode(ByteBuf byteBuf) {
            return byteBuf.readUnsignedShort();
        }

        public void encode(ByteBuf byteBuf, Integer integer) {
            byteBuf.writeShort(integer);
        }
    };
    StreamCodec<ByteBuf, Integer> INT = new StreamCodec<>() {
        public Integer decode(ByteBuf byteBuf) {
            return byteBuf.readInt();
        }

        public void encode(ByteBuf byteBuf, Integer integer) {
            byteBuf.writeInt(integer);
        }
    };
    StreamCodec<ByteBuf, Integer> VAR_INT = new StreamCodec<>() {
        public Integer decode(ByteBuf byteBuf) {
            return new FriendlyByteBuf(byteBuf).readVarInt();
        }

        public void encode(ByteBuf byteBuf, Integer integer) {
            new FriendlyByteBuf(byteBuf).writeVarInt(integer);
        }
    };
    StreamCodec<ByteBuf, Long> VAR_LONG = new StreamCodec<>() {
        public Long decode(ByteBuf byteBuf) {
            return new FriendlyByteBuf(byteBuf).readVarLong();
        }

        public void encode(ByteBuf byteBuf, Long l) {
            new FriendlyByteBuf(byteBuf).writeVarLong(l);
        }
    };
    StreamCodec<ByteBuf, Float> FLOAT = new StreamCodec<>() {
        public Float decode(ByteBuf byteBuf) {
            return byteBuf.readFloat();
        }

        public void encode(ByteBuf byteBuf, Float float_) {
            byteBuf.writeFloat(float_);
        }
    };
    StreamCodec<ByteBuf, Double> DOUBLE = new StreamCodec<>() {
        public Double decode(ByteBuf byteBuf) {
            return byteBuf.readDouble();
        }

        public void encode(ByteBuf byteBuf, Double double_) {
            byteBuf.writeDouble(double_);
        }
    };
    StreamCodec<FriendlyByteBuf, byte[]> BYTE_ARRAY = new StreamCodec<>() {
        public byte[] decode(FriendlyByteBuf friendlyByteBuf) {
            return friendlyByteBuf.readByteArray();
        }

        public void encode(FriendlyByteBuf friendlyByteBuf, byte[] bs) {
            friendlyByteBuf.writeByteArray(bs);
        }
    };
    StreamCodec<FriendlyByteBuf, String> STRING_UTF8 = stringUtf8(32767);
    StreamCodec<FriendlyByteBuf, CompoundTag> COMPOUND_TAG = compoundTagCodec(() -> new NbtAccounter(2097152L));
    StreamCodec<FriendlyByteBuf, CompoundTag> TRUSTED_COMPOUND_TAG = compoundTagCodec(() -> NbtAccounter.UNLIMITED);
    StreamCodec<FriendlyByteBuf, Optional<CompoundTag>> OPTIONAL_COMPOUND_TAG = new StreamCodec<>() {
        public Optional<CompoundTag> decode(FriendlyByteBuf friendlyByteBuf) {
            return Optional.ofNullable(friendlyByteBuf.readNbt());
        }

        public void encode(FriendlyByteBuf friendlyByteBuf, Optional<CompoundTag> optional) {
            friendlyByteBuf.writeNbt(optional.orElse(null));
        }
    };

    static StreamCodec<FriendlyByteBuf, byte[]> byteArray(final int i) {
        return new StreamCodec<>() {
            public byte[] decode(FriendlyByteBuf friendlyByteBuf) {
                return friendlyByteBuf.readByteArray(i);
            }

            public void encode(FriendlyByteBuf friendlyByteBuf, byte[] bs) {
                if (bs.length > i) {
                    throw new EncoderException("ByteArray with size " + bs.length + " is bigger than allowed " + i);
                } else {
                    friendlyByteBuf.writeByteArray(bs);
                }
            }
        };
    }

    static StreamCodec<FriendlyByteBuf, String> stringUtf8(final int i) {
        return new StreamCodec<>() {
            public String decode(FriendlyByteBuf friendlyByteBuf) {
                return friendlyByteBuf.readUtf(i);
            }

            public void encode(FriendlyByteBuf friendlyByteBuf, String string) {
                friendlyByteBuf.writeUtf(string, i);
            }
        };
    }

    static StreamCodec<FriendlyByteBuf, CompoundTag> compoundTagCodec(Supplier<NbtAccounter> supplier) {
        return new StreamCodec<>() {
            public CompoundTag decode(FriendlyByteBuf friendlyByteBuf) {
                CompoundTag tag = friendlyByteBuf.readNbt(supplier.get());
                if (tag == null) {
                    throw new DecoderException("Expected non-null compound tag");
                } else {
                    return tag;
                }
            }

            public void encode(FriendlyByteBuf friendlyByteBuf, CompoundTag tag) {
                friendlyByteBuf.writeNbt(tag);
            }
        };
    }

    static <T> StreamCodec<FriendlyByteBuf, T> fromCodec(Codec<T> codec) {
        return new StreamCodec<>() {
            @Override
            public T decode(FriendlyByteBuf object) {
                return object.readWithCodec(NbtOps.INSTANCE, codec);
            }

            @Override
            public void encode(FriendlyByteBuf object, T object2) {
                object.writeWithCodec(NbtOps.INSTANCE, codec, object2);
            }
        };
    }

    static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(final StreamCodec<B, V> streamCodec) {
        return new StreamCodec<>() {
            public Optional<V> decode(B byteBuf) {
                return byteBuf.readBoolean() ? Optional.of(streamCodec.decode(byteBuf)) : Optional.empty();
            }

            public void encode(B byteBuf, Optional<V> optional) {
                if (optional.isPresent()) {
                    byteBuf.writeBoolean(true);
                    streamCodec.encode(byteBuf, optional.get());
                } else {
                    byteBuf.writeBoolean(false);
                }

            }
        };
    }

    static int readCount(ByteBuf byteBuf, int maxElements) {
        int count = readVarInt(byteBuf);
        if (count > maxElements) {
            throw new DecoderException(count + " elements exceeded max size of: " + maxElements);
        } else {
            return count;
        }
    }

    static void writeCount(ByteBuf byteBuf, int count, int maxElements) {
        if (count > maxElements) {
            throw new EncoderException(count + " elements exceeded max size of: " + maxElements);
        } else {
            writeVarInt(byteBuf, count);
        }
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> intFunction, StreamCodec<? super B, V> streamCodec) {
        return collection(intFunction, streamCodec, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(final IntFunction<C> intFunction, final StreamCodec<? super B, V> streamCodec, final int maxElements) {
        return new StreamCodec<>() {
            public C decode(B byteBuf) {
                int cnt = ByteBufCodecs.readCount(byteBuf, maxElements);
                C collection = intFunction.apply(Math.min(cnt, 65536));

                for(int j = 0; j < cnt; ++j) {
                    collection.add(streamCodec.decode(byteBuf));
                }

                return collection;
            }

            public void encode(B byteBuf, C collection) {
                ByteBufCodecs.writeCount(byteBuf, collection.size(), maxElements);

                for(V object : collection) {
                    streamCodec.encode(byteBuf, object);
                }

            }
        };
    }

    static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> intFunction) {
        return (streamCodec) -> collection(intFunction, streamCodec);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return (streamCodec) -> collection(ArrayList::new, streamCodec);
    }

    static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int i) {
        return (streamCodec) -> collection(ArrayList::new, streamCodec, i);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> intFunction, StreamCodec<? super B, K> streamCodec, StreamCodec<? super B, V> streamCodec2) {
        return map(intFunction, streamCodec, streamCodec2, Integer.MAX_VALUE);
    }

    static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(final IntFunction<? extends M> intFunction, final StreamCodec<? super B, K> streamCodec, final StreamCodec<? super B, V> streamCodec2, final int maxElements) {
        return new StreamCodec<>() {
            public void encode(B byteBuf, M map) {
                ByteBufCodecs.writeCount(byteBuf, map.size(), maxElements);
                map.forEach((object, object2) -> {
                    streamCodec.encode(byteBuf, object);
                    streamCodec2.encode(byteBuf, object2);
                });
            }

            public M decode(B byteBuf) {
                int cnt = ByteBufCodecs.readCount(byteBuf, maxElements);
                M map = intFunction.apply(Math.min(cnt, 65536));

                for (int j = 0; j < cnt; ++j) {
                    K object = streamCodec.decode(byteBuf);
                    V object2 = streamCodec2.decode(byteBuf);
                    map.put(object, object2);
                }

                return map;
            }
        };
    }

    static <B extends ByteBuf, L, R> StreamCodec<B, Either<L, R>> either(final StreamCodec<? super B, L> streamCodec, final StreamCodec<? super B, R> streamCodec2) {
        return new StreamCodec<>() {
            public Either<L, R> decode(B byteBuf) {
                return byteBuf.readBoolean() ? Either.left(streamCodec.decode(byteBuf)) : Either.right(streamCodec2.decode(byteBuf));
            }

            public void encode(B byteBuf, Either<L, R> either) {
                either.ifLeft((object) -> {
                    byteBuf.writeBoolean(true);
                    streamCodec.encode(byteBuf, object);
                }).ifRight((object) -> {
                    byteBuf.writeBoolean(false);
                    streamCodec2.encode(byteBuf, object);
                });
            }
        };
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(final IntFunction<T> intFunction, final ToIntFunction<T> toIntFunction) {
        return new StreamCodec<>() {
            public T decode(ByteBuf byteBuf) {
                int i = readVarInt(byteBuf);
                return intFunction.apply(i);
            }

            public void encode(ByteBuf byteBuf, T object) {
                int i = toIntFunction.applyAsInt(object);
                writeVarInt(byteBuf, i);
            }
        };
    }

    static <T> StreamCodec<ByteBuf, T> idMapper(IdMap<T> idMap) {
        Objects.requireNonNull(idMap);
        return idMapper(idMap::byIdOrThrow, idMap::getId);
    }

    static int readVarInt(ByteBuf buf) {
        int i = 0;
        int j = 0;

        byte b;
        do {
            b = buf.readByte();
            i |= (b & 127) << j++ * 7;
            if (j > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 128) == 128);

        return i;
    }

    static long readVarLong(ByteBuf buf) {
        long l = 0L;
        int i = 0;

        byte b;
        do {
            b = buf.readByte();
            l |= (long)(b & 127) << i++ * 7;
            if (i > 10) {
                throw new RuntimeException("VarLong too big");
            }
        } while((b & 128) == 128);

        return l;
    }

    static ByteBuf writeVarInt(ByteBuf buf, int i) {
        while((i & -128) != 0) {
            buf.writeByte(i & 127 | 128);
            i >>>= 7;
        }

        buf.writeByte(i);
        return buf;
    }

    static ByteBuf writeVarLong(ByteBuf buf, long l) {
        while((l & -128L) != 0L) {
            buf.writeByte((int)(l & 127L) | 128);
            l >>>= 7;
        }

        buf.writeByte((int)l);
        return buf;
    }
}
