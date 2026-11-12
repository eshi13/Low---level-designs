package com.embedcache;

/**
 * Immutable snapshot of cache counters at a point in time.
 */
public final class CacheStats {

    private final long hits;
    private final long misses;
    private final long evictions;
    private final int size;

    public CacheStats(long hits, long misses, long evictions, int size) {
        this.hits = hits;
        this.misses = misses;
        this.evictions = evictions;
        this.size = size;
    }

    public long hits() {
        return hits;
    }

    public long misses() {
        return misses;
    }

    public long evictions() {
        return evictions;
    }

    public int size() {
        return size;
    }

    /**
     * @return hits / (hits + misses), or {@code 0.0} when there are no lookups yet
     */
    public double hitRate() {
        long total = hits + misses;
        if (total == 0L) {
            return 0.0;
        }
        return (double) hits / (double) total;
    }

    @Override
    public String toString() {
        return "CacheStats{hits=" + hits
                + ", misses=" + misses
                + ", evictions=" + evictions
                + ", size=" + size
                + ", hitRate=" + hitRate()
                + '}';
    }
}
