package com.embedcache;

import com.embedcache.eviction.EvictionPolicy;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe in-memory cache with injectable eviction policy, lazy TTL expiry, and stats.
 *
 * <p>Concurrency model (chosen for correctness + learnability): a single
 * {@link ReentrantLock} guards policy + capacity decisions together with map
 * mutations that must stay consistent. Stats counters use atomics so snapshots
 * stay cheap. Libraries like Caffeine use finer techniques for extreme throughput;
 * this is the right first production-safe teaching design.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class InMemoryCache<K, V> implements Cache<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> store = new ConcurrentHashMap<>();
    private final EvictionPolicy<K> policy;
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();
    private final AtomicLong evictions = new AtomicLong();
    private final int maximumSize;
    private final Duration defaultTtl;

    InMemoryCache(EvictionPolicy<K> policy, int maximumSize, Duration defaultTtl) {
        this.policy = Objects.requireNonNull(policy, "policy");
        if (maximumSize <= 0) {
            throw new IllegalArgumentException("maximumSize must be > 0");
        }
        this.maximumSize = maximumSize;
        this.defaultTtl = defaultTtl; // may be null = no default expiry
    }

    @Override
    public V get(K key) {
        Objects.requireNonNull(key, "key");
        lock.lock();
        try {
            CacheEntry<V> entry = store.get(key);
            if (entry == null) {
                misses.incrementAndGet();
                return null;
            }
            if (entry.isExpired()) {
                removeEntryLocked(key);
                misses.incrementAndGet();
                return null;
            }
            policy.recordAccess(key);
            hits.incrementAndGet();
            return entry.getValue();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(K key, V value) {
        put(key, value, defaultTtl);
    }

    @Override
    public void put(K key, V value, Duration ttl) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        long expireAt = toExpireAtMillis(ttl);

        lock.lock();
        try {
            CacheEntry<V> existing = store.get(key);
            if (existing != null) {
                if (existing.isExpired()) {
                    // Treat expired key as gone so capacity/policy stay accurate.
                    removeEntryLocked(key);
                } else {
                    existing.setValue(value);
                    existing.setExpireAtMillis(expireAt);
                    // Refresh counts as an access for LRU/LFU ordering.
                    policy.recordAccess(key);
                    return;
                }
            }

            if (policy.size() >= maximumSize) {
                evictOneLocked();
            }

            store.put(key, new CacheEntry<>(value, expireAt));
            policy.recordInsert(key);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean remove(K key) {
        Objects.requireNonNull(key, "key");
        lock.lock();
        try {
            CacheEntry<V> removed = store.remove(key);
            if (removed == null) {
                return false;
            }
            policy.recordRemoval(key);
            return true;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            store.clear();
            policy.clear();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            return liveSizeLocked();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public CacheStats stats() {
        lock.lock();
        try {
            return new CacheStats(
                    hits.get(),
                    misses.get(),
                    evictions.get(),
                    liveSizeLocked()
            );
        } finally {
            lock.unlock();
        }
    }

    private void evictOneLocked() {
        K victim = policy.evictKey();
        if (victim == null) {
            return;
        }
        store.remove(victim);
        policy.recordRemoval(victim);
        evictions.incrementAndGet();
    }

    private void removeEntryLocked(K key) {
        store.remove(key);
        policy.recordRemoval(key);
    }

    /**
     * Counts non-expired entries and lazily purges expired ones found while scanning.
     * Called under the lock so policy + map stay consistent.
     */
    private int liveSizeLocked() {
        long now = System.currentTimeMillis();
        int live = 0;
        for (var iterator = store.entrySet().iterator(); iterator.hasNext(); ) {
            var mapEntry = iterator.next();
            CacheEntry<V> entry = mapEntry.getValue();
            if (entry.isExpired(now)) {
                iterator.remove();
                policy.recordRemoval(mapEntry.getKey());
            } else {
                live++;
            }
        }
        return live;
    }

    private static long toExpireAtMillis(Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return -1L;
        }
        return System.currentTimeMillis() + ttl.toMillis();
    }
}
