package com.embedcache;

import java.time.Duration;

/**
 * Public contract for an embeddable in-memory cache.
 * Services should depend on this interface, not a concrete implementation.
 *
 * @param <K> key type
 * @param <V> value type
 */
public interface Cache<K, V> {

    /**
     * Returns the value for {@code key}, or {@code null} if missing or expired.
     */
    V get(K key);

    /**
     * Inserts or updates {@code key} using the cache's default TTL.
     */
    void put(K key, V value);

    /**
     * Inserts or updates {@code key} with a per-entry TTL.
     * {@code null} or {@link Duration#ZERO} means the entry never expires.
     */
    void put(K key, V value, Duration ttl);

    /**
     * Removes {@code key} if present.
     *
     * @return {@code true} if an entry was removed
     */
    boolean remove(K key);

    /**
     * Removes all entries and resets eviction-policy state.
     */
    void clear();

    /**
     * Returns the number of live (non-expired) entries.
     */
    int size();

    /**
     * Returns an immutable snapshot of hit/miss/eviction counters and current size.
     */
    CacheStats stats();
}
