# Embed Cache

Embeddable, thread-safe Java in-memory cache with runtime-selectable **LRU** / **LFU** eviction, per-entry TTL, and hit/miss stats.

```java
Cache<String, User> cache = CacheBuilder.<String, User>newBuilder()
    .maximumSize(1000)
    .evictionPolicy(EvictionPolicyType.LFU)
    .defaultTtl(Duration.ofMinutes(10))
    .build();

cache.put("u1", user);
User u = cache.get("u1");
CacheStats stats = cache.stats();
```

## Build & test

```bash
mvn test
```

Requires Java 17+.

## Design

| Piece | Role |
|---|---|
| `Cache` | Public API interface |
| `EvictionPolicy` | Strategy for LRU vs LFU |
| `CacheBuilder` | Fluent config + validation |
| `InMemoryCache` | Store, lock, TTL, stats |
| `CacheEntry` / `CacheStats` | Entry holder / immutable snapshot |

Concurrency: a single `ReentrantLock` keeps the map and eviction policy consistent. TTL uses lazy expiry on `get` / `put` / `size`.
