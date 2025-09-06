package com.limitra.cache;

public record MetricsSnapshot(long hits, long misses, long evictedByTtl, long evictedByCapacity)
        implements CacheMetrics {}
