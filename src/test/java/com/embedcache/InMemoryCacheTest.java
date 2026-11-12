package com.embedcache;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryCacheTest {

    @Test
    void putAndGetRoundTrip() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("k1", "v1");
        assertEquals("v1", cache.get("k1"));
        assertEquals(1, cache.size());
    }

    @Test
    void missIncrementsStats() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        assertNull(cache.get("missing"));
        CacheStats stats = cache.stats();
        assertEquals(0, stats.hits());
        assertEquals(1, stats.misses());
        assertEquals(0.0, stats.hitRate());
    }

    @Test
    void hitIncrementsStats() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("k", "v");
        assertEquals("v", cache.get("k"));
        CacheStats stats = cache.stats();
        assertEquals(1, stats.hits());
        assertEquals(0, stats.misses());
        assertEquals(1.0, stats.hitRate());
    }

    @Test
    void lruEvictsLeastRecentlyUsedWhenAtCapacity() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(2)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a"); // a becomes most recent
        cache.put("c", "3"); // should evict b

        assertEquals("1", cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals("3", cache.get("c"));
        assertEquals(1, cache.stats().evictions());
    }

    @Test
    void lfuEvictsLeastFrequentlyUsedWhenAtCapacity() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(2)
                .evictionPolicy(EvictionPolicyType.LFU)
                .build();

        cache.put("a", "1");
        cache.put("b", "2");
        cache.get("a"); // a freq=2, b freq=1
        cache.put("c", "3"); // should evict b

        assertEquals("1", cache.get("a"));
        assertNull(cache.get("b"));
        assertEquals("3", cache.get("c"));
        assertEquals(1, cache.stats().evictions());
    }

    @Test
    void overwriteDoesNotGrowSize() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(2)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("a", "1");
        cache.put("a", "2");
        assertEquals(1, cache.size());
        assertEquals("2", cache.get("a"));
        assertEquals(0, cache.stats().evictions());
    }

    @Test
    void removeDeletesEntry() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("a", "1");
        assertTrue(cache.remove("a"));
        assertFalse(cache.remove("a"));
        assertNull(cache.get("a"));
        assertEquals(0, cache.size());
    }

    @Test
    void clearEmptiesCache() {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LFU)
                .build();

        cache.put("a", "1");
        cache.put("b", "2");
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("a"));
    }

    @Test
    void defaultTtlExpiresLazilyOnGet() throws InterruptedException {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .defaultTtl(Duration.ofMillis(50))
                .build();

        cache.put("a", "1");
        assertEquals("1", cache.get("a"));
        Thread.sleep(80);
        assertNull(cache.get("a"));
        assertEquals(1, cache.stats().misses());
    }

    @Test
    void perEntryTtlOverridesDefault() throws InterruptedException {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(10)
                .evictionPolicy(EvictionPolicyType.LRU)
                .defaultTtl(Duration.ofMillis(50))
                .build();

        cache.put("short", "s");
        cache.put("forever", "f", Duration.ZERO);
        Thread.sleep(80);

        assertNull(cache.get("short"));
        assertEquals("f", cache.get("forever"));
    }

    @Test
    void expiredEntryFreesCapacityOnPut() throws InterruptedException {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(1)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("a", "1", Duration.ofMillis(40));
        Thread.sleep(60);
        cache.put("b", "2"); // a is expired; should not need eviction of a live key

        assertEquals("2", cache.get("b"));
        // Eviction may or may not increment depending on whether put saw expired first;
        // size must remain within capacity either way.
        assertEquals(1, cache.size());
    }

    @Test
    void builderRequiresMaximumSizeAndPolicy() {
        assertThrows(IllegalStateException.class,
                () -> CacheBuilder.<String, String>newBuilder()
                        .evictionPolicy(EvictionPolicyType.LRU)
                        .build());

        assertThrows(IllegalStateException.class,
                () -> CacheBuilder.<String, String>newBuilder()
                        .maximumSize(10)
                        .build());

        assertThrows(IllegalArgumentException.class,
                () -> CacheBuilder.<String, String>newBuilder().maximumSize(0));
    }
}
