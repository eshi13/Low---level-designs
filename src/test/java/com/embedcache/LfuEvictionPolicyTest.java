package com.embedcache;

import com.embedcache.eviction.LfuEvictionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LfuEvictionPolicyTest {

    @Test
    void evictsLeastFrequentlyUsed() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordInsert("c");

        policy.recordAccess("a");
        policy.recordAccess("a");
        policy.recordAccess("b");

        // c:1, b:2, a:3 → evict c
        assertEquals("c", policy.evictKey());
    }

    @Test
    void tiesBrokenByOldestInBucket() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordInsert("c");

        // All at frequency 1; oldest insert wins eviction.
        assertEquals("a", policy.evictKey());
        policy.recordRemoval("a");
        assertEquals("b", policy.evictKey());
    }

    @Test
    void accessBumpsFrequencyAndUpdatesMin() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");

        policy.recordAccess("a");
        assertEquals("b", policy.evictKey());

        policy.recordAccess("b");
        policy.recordAccess("b");
        // a:2, b:3
        assertEquals("a", policy.evictKey());
    }

    @Test
    void removalRecomputesMinFrequency() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordAccess("b");

        // a:1, b:2 — remove the only freq-1 key
        policy.recordRemoval("a");
        assertEquals(1, policy.size());
        assertEquals("b", policy.evictKey());
    }

    @Test
    void clearEmptiesPolicy() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordAccess("a");
        policy.clear();

        assertEquals(0, policy.size());
        assertNull(policy.evictKey());
    }

    @Test
    void accessUnknownKeyFails() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        assertThrows(IllegalArgumentException.class, () -> policy.recordAccess("missing"));
    }

    @Test
    void duplicateInsertFails() {
        LfuEvictionPolicy<String> policy = new LfuEvictionPolicy<>();
        policy.recordInsert("a");
        assertThrows(IllegalArgumentException.class, () -> policy.recordInsert("a"));
    }
}
