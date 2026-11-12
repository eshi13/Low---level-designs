package com.embedcache;

/**
 * Holds a cached value and optional absolute expiry time.
 *
 * @param <V> value type
 */
public final class CacheEntry<V> {

    private V value;
    /** Absolute expiry epoch millis; {@code -1} means never expires. */
    private long expireAtMillis;

    public CacheEntry(V value, long expireAtMillis) {
        this.value = value;
        this.expireAtMillis = expireAtMillis;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public long getExpireAtMillis() {
        return expireAtMillis;
    }

    public void setExpireAtMillis(long expireAtMillis) {
        this.expireAtMillis = expireAtMillis;
    }

    /**
     * @return {@code true} if this entry has a TTL and that time has passed
     */
    public boolean isExpired() {
        return expireAtMillis >= 0 && System.currentTimeMillis() >= expireAtMillis;
    }

    /**
     * Same as {@link #isExpired()} but uses a caller-supplied clock reading
     * so batch checks stay consistent under a single lock.
     */
    public boolean isExpired(long nowMillis) {
        return expireAtMillis >= 0 && nowMillis >= expireAtMillis;
    }
}
