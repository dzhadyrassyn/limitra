package com.limitra;

import com.limitra.cache.SimpleTTLCache;
import com.limitra.limiter.RateLimiter;
import com.limitra.limiter.TokenBucketRateLimiter;
import com.limitra.metrics.CacheMetrics;
import com.limitra.time.SystemTimeProvider;
import com.limitra.time.TimeProvider;

public class IntegrationDemo {
    public static void main(String[] args) throws InterruptedException {

        TimeProvider time = new SystemTimeProvider();
        SimpleTTLCache<String, String> cache = new SimpleTTLCache<>(time, 100);
        RateLimiter limiter = new TokenBucketRateLimiter(time, 5, 2.0);

        for (int i = 0; i < 20; i++) {
            if (!limiter.tryAcquire()) {
                System.out.println("Request " + i + " is denied by rate limiter");
            } else {
                String key = "data:" + (i % 3);
                var j = i;
                cache.get(key)
                        .ifPresentOrElse(
                                val -> {
                                    long t = System.currentTimeMillis() % 100_000;
                                    System.out.println(
                                            "["
                                                    + t
                                                    + "ms] Request "
                                                    + j
                                                    + " served from cache: "
                                                    + val);
                                },
                                () -> {
                                    String value = "Value@" + j;
                                    cache.put(key, value, 2000);
                                    System.out.println(
                                            "Request " + j + " fetched and cached: " + value);
                                });
            }

            Thread.sleep(200);
        }

        CacheMetrics cm = cache.metricsSnapshot();
        System.out.println("\n=== Metrics ===");
        System.out.println(
                "Cache: hits="
                        + cm.hits()
                        + ", misses="
                        + cm.misses()
                        + ", ttlEvictions="
                        + cm.evictedByTtl()
                        + ", capacityEvictions="
                        + cm.evictedByCapacity());
    }
}
