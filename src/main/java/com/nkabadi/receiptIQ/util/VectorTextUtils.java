package com.nkabadi.receiptIQ.util;

import java.util.StringJoiner;

public final class VectorTextUtils {
    private VectorTextUtils() {
    }

    public static String toVectorText(float[] vector) {
        StringJoiner joiner = new StringJoiner(",", "[", "]");
        for (float value : vector) {
            joiner.add(Float.toString(value));
        }
        return joiner.toString();
    }
}
