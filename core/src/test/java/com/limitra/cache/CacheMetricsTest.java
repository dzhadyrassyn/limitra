package com.limitra.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CacheMetricsTest {

    @Test
    void metrics_hitOnPresent_missOnAbsent() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider());
        cache.put("hit", 1);

        // When
        cache.get("miss");
        cache.get("hit");

        // Then
        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(1, snapshotAfter.misses());
        assertEquals(1, snapshotAfter.hits());
    }

    @Test
    void metrics_expiredOnGet_countsMiss_andTtlEviction() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("miss-and-evictedByTtl", 1, 100);

        // When
        time.advanceMillis(101);
        Optional<Integer> item = cache.get("miss-and-evictedByTtl");

        // Then
        assertTrue(item.isEmpty());

        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotAfter.hits());
        assertEquals(1, snapshotAfter.misses());
        assertEquals(1, snapshotAfter.evictedByTtl());
    }

    @Test
    void metrics_expiredOnSizeCleanup_countsTtlEviction() {

        // Given
        FakeTimeProvider time =new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("evictedByTtl", 1, 100);

        // When
        time.advanceMillis(101);
        long size = cache.size();

        // Then
        assertEquals(0, size);

        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotAfter.misses());
        assertEquals(1, snapshotAfter.evictedByTtl());
    }

    @Test
    void metrics_onRemove_noTtlEviction() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new  FakeTimeProvider());
        cache.put("eternalKey", 1);

        // When
        boolean removed = cache.remove("eternalKey");

        // Then
        assertTrue(removed);

        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotAfter.evictedByTtl());
    }

    @Test
    void metrics_overwriteUnexpired_doesNotCountEviction() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("notEvictedByTtl", 1, 100);

        // When
        cache.put("notEvictedByTtl", 2, 200);
        time.advanceMillis(101);

        // Then
        Optional<Integer> a = cache.get("notEvictedByTtl");
        assertTrue(a.isPresent());
        assertEquals(2, a.get());

        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotAfter.hits());
        assertEquals(0, snapshotAfter.misses());
        assertEquals(0, snapshotAfter.evictedByTtl());
    }

    @Test
    void ttlEviction_isCountedOnce_evenIfGetAndSizeBothSeeExpired() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("evictedByTtlOnce", 1, 100);

        // When
        time.advanceMillis(101);

        // Then
        long size = cache.size();
        Optional<Integer> item = cache.get("evictedByTtlOnce");

        assertEquals(0, size);
        assertTrue(item.isEmpty());

        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotAfter.hits());
        assertEquals(1, snapshotAfter.misses());
        assertEquals(1, snapshotAfter.evictedByTtl());
    }

    @Test
    void testMetricsImmutability() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new  FakeTimeProvider());
        CacheMetrics snapshotBefore = cache.metricsSnapshot();

        // When
        cache.put("hit", 1);
        cache.get("hit");

        // Then
        CacheMetrics snapshotAfter = cache.metricsSnapshot();
        assertEquals(0, snapshotBefore.hits());
        assertTrue(snapshotAfter.hits() > snapshotBefore.hits());
    }
}