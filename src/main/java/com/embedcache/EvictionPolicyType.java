package com.embedcache;

import com.embedcache.eviction.EvictionPolicy;
import com.embedcache.eviction.LfuEvictionPolicy;
import com.embedcache.eviction.LruEvictionPolicy;

/**
 * Runtime-selectable eviction algorithms.
 * The builder uses this enum as a small factory for policy instances.
 */
public enum EvictionPolicyType {
    LRU,
    LFU;

    /**
     * Creates a fresh policy instance for the chosen algorithm.
     */
    public <K> EvictionPolicy<K> createPolicy() {
        return switch (this) {
            case LRU -> new LruEvictionPolicy<>();
            case LFU -> new LfuEvictionPolicy<>();
        };
    }
}
