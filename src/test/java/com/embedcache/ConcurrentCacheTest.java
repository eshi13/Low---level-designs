package com.embedcache;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrentCacheTest {

    @Test
    void concurrentPutsNeverExceedMaximumSize() throws Exception {
        int maxSize = 50;
        Cache<String, Integer> cache = CacheBuilder.<String, Integer>newBuilder()
                .maximumSize(maxSize)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        int threads = 8;
        int putsPerThread = 500;
        runConcurrently(threads, () -> {
            for (int i = 0; i < putsPerThread; i++) {
                String key = Thread.currentThread().getId() + "-" + i;
                cache.put(key, i);
                assertTrue(cache.size() <= maxSize);
            }
        });

        assertTrue(cache.size() <= maxSize);
        CacheStats stats = cache.stats();
        assertTrue(stats.evictions() > 0);
        assertEquals(cache.size(), stats.size());
    }

    @Test
    void concurrentMixedOpsRemainConsistent() throws Exception {
        int maxSize = 100;
        Cache<Integer, String> cache = CacheBuilder.<Integer, String>newBuilder()
                .maximumSize(maxSize)
                .evictionPolicy(EvictionPolicyType.LFU)
                .defaultTtl(Duration.ofSeconds(30))
                .build();

        int threads = 10;
        int opsPerThread = 1000;
        AtomicInteger errors = new AtomicInteger();

        runConcurrently(threads, () -> {
            long id = Thread.currentThread().getId();
            for (int i = 0; i < opsPerThread; i++) {
                int key = (int) ((id * opsPerThread + i) % (maxSize * 2));
                try {
                    switch (i % 4) {
                        case 0 -> cache.put(key, "v-" + key);
                        case 1 -> cache.get(key);
                        case 2 -> cache.remove(key);
                        default -> cache.put(key, "v2-" + key, Duration.ofMillis(500));
                    }
                    if (cache.size() > maxSize) {
                        errors.incrementAndGet();
                    }
                } catch (RuntimeException ex) {
                    errors.incrementAndGet();
                    throw ex;
                }
            }
        });

        assertEquals(0, errors.get());
        assertTrue(cache.size() <= maxSize);

        CacheStats stats = cache.stats();
        assertTrue(stats.hits() + stats.misses() > 0);
        assertTrue(stats.hitRate() >= 0.0 && stats.hitRate() <= 1.0);
        assertEquals(cache.size(), stats.size());
    }

    @Test
    void concurrentGetsDoNotCorruptLruOrdering() throws Exception {
        Cache<String, String> cache = CacheBuilder.<String, String>newBuilder()
                .maximumSize(3)
                .evictionPolicy(EvictionPolicyType.LRU)
                .build();

        cache.put("a", "1");
        cache.put("b", "2");
        cache.put("c", "3");

        runConcurrently(16, () -> {
            for (int i = 0; i < 200; i++) {
                cache.get("a");
            }
        });

        // a was heavily accessed; inserting d/e should prefer evicting colder keys.
        cache.put("d", "4");
        cache.put("e", "5");

        assertEquals("1", cache.get("a"));
        assertTrue(cache.size() <= 3);
    }

    private static void runConcurrently(int threads, Runnable task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>(threads);
        try {
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    task.run();
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(60, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
