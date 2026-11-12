package com.embedcache;

import com.embedcache.eviction.LruEvictionPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LruEvictionPolicyTest {

    @Test
    void evictsLeastRecentlyUsed() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordInsert("c");

        assertEquals("a", policy.evictKey());

        policy.recordAccess("a");
        assertEquals("b", policy.evictKey());
    }

    @Test
    void accessMovesKeyToMostRecent() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordInsert("c");

        policy.recordAccess("a");
        policy.recordAccess("b");

        assertEquals("c", policy.evictKey());
        policy.recordRemoval("c");
        assertEquals("a", policy.evictKey());
        policy.recordRemoval("a");
        assertEquals("b", policy.evictKey());
    }

    @Test
    void removalUpdatesEvictionOrder() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.recordInsert("c");

        policy.recordRemoval("a");
        assertEquals(2, policy.size());
        assertEquals("b", policy.evictKey());
    }

    @Test
    void clearEmptiesPolicy() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        policy.recordInsert("a");
        policy.recordInsert("b");
        policy.clear();

        assertEquals(0, policy.size());
        assertNull(policy.evictKey());
    }

    @Test
    void accessUnknownKeyFails() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        assertThrows(IllegalArgumentException.class, () -> policy.recordAccess("missing"));
    }

    @Test
    void duplicateInsertFails() {
        LruEvictionPolicy<String> policy = new LruEvictionPolicy<>();
        policy.recordInsert("a");
        assertThrows(IllegalArgumentException.class, () -> policy.recordInsert("a"));
    }
}
