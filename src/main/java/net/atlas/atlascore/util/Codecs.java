package net.atlas.atlascore.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.function.Function;

public class Codecs {
    public static Codec<Double> doubleRangeMinInclusiveWithMessage(double d, double e, Function<Double, String> function) {
        return Codec.DOUBLE.validate((double_) -> double_.compareTo(d) >= 0 && double_.compareTo(e) <= 0 ? DataResult.success(double_) : DataResult.error(() -> function.apply(double_)));
    }

    public static Codec<Double> doubleRangeMinExclusiveWithMessage(double d, double e, Function<Double, String> function) {
        return Codec.DOUBLE.validate((double_) -> double_.compareTo(d) > 0 && double_.compareTo(e) <= 0 ? DataResult.success(double_) : DataResult.error(() -> function.apply(double_)));
    }

    public static Codec<Double> doubleRange(double d, double e) {
        return doubleRangeMinInclusiveWithMessage(d, e, (double_) -> "Value must be within range [" + d + ";" + e + "]: " + double_);
    }
}