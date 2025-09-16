package com.limitra.metrics;

/**
 * Represents an immutable, read-only snapshot of the cache’s counters. To observe updated values,
 * request a new snapshot from the cache implementation.
 */
public interface CacheMetrics {

    long hits();

    long misses();

    long evictedByTtl();

    long evictedByCapacity();
}
