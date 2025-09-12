# Limitra

Limitra is a lightweight, high-performance library combining:
- **In-memory cache** with TTL and LRU eviction.
- **Token-bucket rate limiter** for controlling request throughput.
- **REST API demo** (planned).

---

## Features
- ✅ Thread-safe in-memory cache
- ✅ TTL support (per-entry expiry)
- ✅ LRU eviction (capacity-bounded)
- ✅ Metrics (hits, misses, evictions)
- ✅ Token-bucket rate limiter (capacity + refill rate)
- 🚧 REST API example with Spring Boot (planned)
- 🚧 Docker/Kubernetes deployment (planned)

---

## Design Choices

- **Null policy**  
  Rejects null keys/values; throws `NullPointerException` or `IllegalArgumentException`.

- **Time model**  
  Uses a `TimeProvider` abstraction. Expiry and refill decisions are based on monotonic nanoseconds, not wall-clock.

- **Thread safety**  
  All operations are safe under concurrency. No compound atomicity across multiple ops.  
  Rate limiter uses synchronization; no fairness guarantees between threads.

- **Cache entry lifetime**  
  Eternal (`put(k,v)`) or TTL-bound (`put(k,v,ttlMillis)`). Overwrites reset TTL.

- **Cache eviction policy**  
  Capacity-bounded with **Least Recently Used (LRU)**.  
  Expired entries → TTL eviction; live entries forced out → capacity eviction.

- **Cache metrics**  
  Snapshot via `metricsSnapshot()`: hits, misses, `evictedByTtl`, `evictedByCapacity`.

- **Cache size semantics**  
  `size()` counts only non-expired entries. May run lazy cleanup; result is eventually consistent.

- **Rate limiter model**  
  Token bucket:
  - Starts full (burst up to `capacity`)
  - Refills at steady `permitsPerSecond` (may be fractional)
  - Requests consume permits; if not enough, call returns `false` immediately
  - Requests larger than capacity always fail

---

## Usage Examples

### Cache
```java
import com.limitra.cache.*;

public class ExampleCache {
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
```

### Rate Limiter
```java
import com.limitra.limiter.*;

public class ExampleLimiter {
    public static void main(String[] args) {
        RateLimiter limiter = new TokenBucketRateLimiter(
            new SystemTimeProvider(),
            5,    // capacity (burst size)
            2.0   // refill rate per second
        );

        for (int i = 0; i < 10; i++) {
            if (limiter.tryAcquire()) {
                System.out.println("Request " + i + " allowed");
            } else {
                System.out.println("Request " + i + " denied");
            }
        }
    }
}
```