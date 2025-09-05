# Limitra

Limitra is a lightweight, high-performance library combining:
- **In-memory cache** with TTL and LRU eviction.
- **Rate limiter** (coming soon).
- **REST API demo** (coming soon).

---

## Features
- ✅ Thread-safe in-memory cache
- ✅ TTL support (per-entry expiry)
- ✅ LRU eviction (capacity-bounded)
- ✅ Metrics (hits, misses, evictions)
- 🚧 Rate limiter module (planned)
- 🚧 REST API example with Spring Boot (planned)
- 🚧 Docker/Kubernetes deployment (planned)

---

## Design Choices

- **Null policy**  
  Rejects null keys/values; throws `NullPointerException` or `IllegalArgumentException`.

- **Time model**  
  Uses `TimeProvider` abstraction. Expiry decisions are monotonic nanoseconds, not wall-clock.

- **Thread safety**  
  All operations are safe under concurrency. No compound atomicity across multiple ops.

- **Entry lifetime**  
  Eternal (`put(k,v)`) or TTL-bound (`put(k,v,ttlMillis)`). Overwrites reset TTL.

- **Eviction policy**  
  Capacity-bounded with **Least Recently Used (LRU)**.  
  Expired entries → TTL eviction; live entries forced out → capacity eviction.

- **Metrics**  
  Snapshot via `metricsSnapshot()`: hits, misses, `evictedByTtl`, `evictedByCapacity`.

- **Size semantics**  
  `size()` counts only non-expired entries. May run lazy cleanup; result is eventually consistent.

---

## Usage Example

```java
import com.limitra.cache.*;

public class Example {
    public static void main(String[] args) {
        TimeProvider time = new SystemTimeProvider();
        Cache<String, String> cache = new SimpleTTLCache<>(time, 2);

        cache.put("a", "alpha");
        cache.put("b", "bravo", 100); // TTL 100 ms

        System.out.println(cache.get("a")); // Optional[alpha]

        try { Thread.sleep(120); } catch (InterruptedException e) {}
        System.out.println(cache.get("b")); // Optional.empty (expired)

        CacheMetrics m = ((SimpleTTLCache<String, String>) cache).metricsSnapshot();
        System.out.println("hits=" + m.hits() +
                           ", misses=" + m.misses() +
                           ", ttlEvictions=" + m.evictedByTtl() +
                           ", capacityEvictions=" + m.evictedByCapacity());
    }
}