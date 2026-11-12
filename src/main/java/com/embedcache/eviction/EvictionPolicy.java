package com.embedcache.eviction;

/**
 * Strategy for deciding which key to drop when the cache is at capacity.
 * The cache never branches on LRU vs LFU — it only calls this contract.
 *
 * @param <K> key type
 */
public interface EvictionPolicy<K> {

    /**
     * Called on a successful {@code get} (and optionally on overwrite refresh).
     */
    void recordAccess(K key);

    /**
     * Called when a new key is inserted into the cache.
     */
    void recordInsert(K key);

    /**
     * Called when a key is removed, overwritten, or expired out of the policy.
     */
    void recordRemoval(K key);

    /**
     * Returns the next key that should be evicted, without removing it from the policy.
     * Implementations typically return the least-valuable key; the cache then
     * calls {@link #recordRemoval(Object)} after deleting from the store.
     *
     * @return key to evict, or {@code null} if the policy is empty
     */
    K evictKey();

    /**
     * Number of keys currently tracked by this policy.
     */
    int size();

    /**
     * Clears all tracked keys.
     */
    void clear();
}
