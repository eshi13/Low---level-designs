package com.embedcache;

import com.embedcache.eviction.EvictionPolicy;

import java.time.Duration;
import java.util.Objects;

/**
 * Fluent builder for {@link InMemoryCache}. Validates config and creates the
 * eviction policy via {@link EvictionPolicyType} at build time.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class CacheBuilder<K, V> {

    private Integer maximumSize;
    private EvictionPolicyType evictionPolicyType;
    private Duration defaultTtl;

    private CacheBuilder() {
    }

    public static <K, V> CacheBuilder<K, V> newBuilder() {
        return new CacheBuilder<>();
    }

    public CacheBuilder<K, V> maximumSize(int maximumSize) {
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be > 0");
        }
        this.maximumSize = maximumSize;
        return this;
    }

    public CacheBuilder<K, V> evictionPolicy(EvictionPolicyType evictionPolicyType) {
        this.evictionPolicyType = Objects.requireNonNull(evictionPolicyType, "evictionPolicyType");
        return this;
    }

    /**
     * Optional default TTL applied by {@link Cache#put(Object, Object)}.
     * {@code null} or {@link Duration#ZERO} means entries do not expire by default.
     */
    public CacheBuilder<K, V> defaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
        return this;
    }

    public Cache<K, V> build() {
        if (maximumSize == null) {
            throw new IllegalStateException("maximumSize is required");
        }
        if (evictionPolicyType == null) {
            throw new IllegalStateException("evictionPolicy is required");
        }
        EvictionPolicy<K> policy = evictionPolicyType.createPolicy();
        Duration ttl = normalizeTtl(defaultTtl);
        return new InMemoryCache<>(policy, maximumSize, ttl);
    }

    private static Duration normalizeTtl(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return null;
        }
        return ttl;
    }
}
