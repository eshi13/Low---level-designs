package com.embedcache.eviction;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;

/**
 * Least-Frequently-Used eviction with O(1) frequency buckets.
 * On frequency ties, the oldest key in that bucket is evicted (LinkedHashSet order).
 *
 * @param <K> key type
 */
public final class LfuEvictionPolicy<K> implements EvictionPolicy<K> {

    private final Map<K, Integer> keyToFreq = new HashMap<>();
    private final Map<Integer, LinkedHashSet<K>> freqToKeys = new HashMap<>();
    private int minFreq = 0;

    @Override
    public void recordAccess(K key) {
        Objects.requireNonNull(key, "key");
        Integer freq = keyToFreq.get(key);
        if (freq == null) {
            throw new IllegalArgumentException("Unknown key for access: " + key);
        }
        removeFromBucket(key, freq);
        int nextFreq = freq + 1;
        keyToFreq.put(key, nextFreq);
        addToBucket(key, nextFreq);
        if (freq == minFreq && isBucketEmpty(freq)) {
            minFreq = nextFreq;
        }
    }

    @Override
    public void recordInsert(K key) {
        Objects.requireNonNull(key, "key");
        if (keyToFreq.containsKey(key)) {
            throw new IllegalArgumentException("Key already present: " + key);
        }
        keyToFreq.put(key, 1);
        addToBucket(key, 1);
        minFreq = 1;
    }

    @Override
    public void recordRemoval(K key) {
        Objects.requireNonNull(key, "key");
        Integer freq = keyToFreq.remove(key);
        if (freq == null) {
            return;
        }
        removeFromBucket(key, freq);
        if (freq == minFreq && isBucketEmpty(freq)) {
            // Next min will be recomputed on next insert/access/evict if needed.
            // When empty, minFreq is irrelevant until the next insert.
            if (keyToFreq.isEmpty()) {
                minFreq = 0;
            } else {
                // Find the new minimum frequency among remaining keys.
                minFreq = keyToFreq.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            }
        }
    }

    @Override
    public K evictKey() {
        if (keyToFreq.isEmpty()) {
            return null;
        }
        LinkedHashSet<K> bucket = freqToKeys.get(minFreq);
        if (bucket == null || bucket.isEmpty()) {
            // Defensive: recompute minFreq if buckets drifted.
            minFreq = keyToFreq.values().stream().mapToInt(Integer::intValue).min().orElse(0);
            bucket = freqToKeys.get(minFreq);
            if (bucket == null || bucket.isEmpty()) {
                return null;
            }
        }
        return bucket.iterator().next();
    }

    @Override
    public int size() {
        return keyToFreq.size();
    }

    @Override
    public void clear() {
        keyToFreq.clear();
        freqToKeys.clear();
        minFreq = 0;
    }

    private void addToBucket(K key, int freq) {
        freqToKeys.computeIfAbsent(freq, ignored -> new LinkedHashSet<>()).add(key);
    }

    private void removeFromBucket(K key, int freq) {
        LinkedHashSet<K> bucket = freqToKeys.get(freq);
        if (bucket == null) {
            return;
        }
        bucket.remove(key);
        if (bucket.isEmpty()) {
            freqToKeys.remove(freq);
        }
    }

    private boolean isBucketEmpty(int freq) {
        LinkedHashSet<K> bucket = freqToKeys.get(freq);
        return bucket == null || bucket.isEmpty();
    }
}
