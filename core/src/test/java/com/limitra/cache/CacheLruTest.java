package com.limitra.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CacheLruTest {

    @Test
    void constructor_invalidCapacity_throws() {

        assertThrows(IllegalArgumentException.class, () -> {
            new SimpleTTLCache<>(new FakeTimeProvider(), 0L);
        });
    }

    @Test
    void capacity_exactlyN_items_fit_without_eviction() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2L);

        // When
        cache.put("a", 1);
        cache.put("b", 2);

        // Then
        Optional<Integer> item1 = cache.get("a");
        Optional<Integer> item2 = cache.get("b");

        assertTrue(item1.isPresent());
        assertTrue(item2.isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void lru_capacityExceeded_evictsLeastRecentlyUsed() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2L);
        cache.put("evictedByCapacity", 1);
        cache.put("b", 2);

        // When
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("evictedByCapacity").isEmpty());
        assertTrue(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(1, afterSnapshot.evictedByCapacity());
    }

    @Test
    void lru_overwriteCountsAsAccess_notExtraEntry() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2L);
        cache.put("a", 1);
        cache.put("evictedByCapacity", 2);

        // When
        cache.put("a", 3);
        cache.put("c", 4);

        // Then
        assertTrue(cache.get("evictedByCapacity").isEmpty());
        assertTrue(cache.get("a").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(1, afterSnapshot.evictedByCapacity());
    }

    @Test
    void lru_expiredCandidates_removedAsTtlEviction_notCapacity() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2L);
        cache.put("expiredByTtl", 1, 100);
        cache.put("b", 2);

        // When
        time.advanceMillis(120);
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("expiredByTtl").isEmpty());
        assertTrue(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(1, afterSnapshot.evictedByTtl());
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void lru_whenMultipleExpired_onlyCountRealRemovals_once() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2L);
        cache.put("evictedByTtl1", 1, 50);
        cache.put("evictedByTtl2", 2, 50);

        // When
        time.advanceMillis(60);
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("evictedByTtl1").isEmpty());
        assertTrue(cache.get("evictedByTtl2").isEmpty());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(2, afterSnapshot.evictedByTtl());
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void lru_getOnExpiredKey_countsTtlEviction_andDoesNotAffectCapacityCounters() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2L);
        cache.put("evictedAndMissed", 1, 50);
        cache.put("b", 2);

        // When
        time.advanceMillis(60);
        Optional<Integer> item1 = cache.get("evictedAndMissed");
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("evictedAndMissed").isEmpty());
        assertTrue(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(1, afterSnapshot.misses());
        assertEquals(1, afterSnapshot.evictedByTtl());
        assertEquals(0, afterSnapshot.evictedByCapacity());

    }
}
