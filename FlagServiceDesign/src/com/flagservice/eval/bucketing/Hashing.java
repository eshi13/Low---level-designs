package com.flagservice.eval.bucketing;

final class Hashing {
    private Hashing() {
    }

    /**
     * Stable non-crypto hash mapped to [0, 99].
     * Production systems would typically use Murmur3/xxHash; String.hashCode is sufficient here.
     */
    static int bucket(String material) {
        int hash = material.hashCode();
        // mask to avoid Integer.MIN_VALUE abs overflow and keep non-negative
        int positive = hash & 0x7fffffff;
        return positive % 100;
    }
}
