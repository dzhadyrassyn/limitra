package com.limitra.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class CacheLruTest {

    @Test
    void constructor_invalidCapacity_throws() {

        assertThrows(IllegalArgumentException.class, () -> {
            new SimpleTTLCache<>(new FakeTimeProvider(), 0);
        });
    }

    @Test
    void capacity_exactlyN_items_fit_without_eviction() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2);

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
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2);
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
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2);
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
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2);
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
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2);
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
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2);
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

    @Test
    void size_reflects_liveEntries_afterEvictions() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2);
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
    void size_excludesExpired_then_capacity_adds_new() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(time, 2);
        cache.put("evictedByTtl", 1, 50);
        cache.put("b", 2);

        // When
        time.advanceMillis(60);
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("evictedByTtl").isEmpty());
        assertTrue(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(1, afterSnapshot.evictedByTtl());
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void touchingOnlyNewKey_doesNotEvict_whenWithinCapacity() {

        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 3);
        cache.put("a", 1);
        cache.put("b", 2);

        // When
        cache.get("b");
        cache.get("a");
        cache.put("c", 3);

        // Then
        assertTrue(cache.get("a").isPresent());
        assertTrue(cache.get("b").isPresent());
        assertTrue(cache.get("c").isPresent());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void putSameKey_twice_doesNotTemporarilyExceedCapacity() {

        // Given
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), 2);
        cache.put("a", 1);
        cache.put("a", 2);
        cache.put("b", 3);

        // Then
        assertTrue(cache.get("a").isPresent());
        assertTrue(cache.get("b").isPresent());
        assertEquals(2, cache.size());

        CacheMetrics afterSnapshot = cache.metricsSnapshot();
        assertEquals(0, afterSnapshot.evictedByCapacity());
    }

    @Test
    void concurrentAccess_capacityNeverExceeded() throws InterruptedException {

        // Given
        int capacity = 64;
        SimpleTTLCache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider(), capacity);

        Runnable task = () -> {
            for (int i = 0; i < 10_000; i++) {
                String key = "k" + ThreadLocalRandom.current().nextInt(256);
                if (ThreadLocalRandom.current().nextBoolean()) {
                    cache.put(key, i);
                } else {
                    cache.get(key);
                }
            }
        };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        Thread thread3 = new Thread(task);
        Thread thread4 = new Thread(task);

        // When
        thread1.start(); thread2.start(); thread3.start(); thread4.start();
        thread1.join();  thread2.join(); thread3.join(); thread4.join();

        // Then
        assertTrue(cache.size() <= capacity);
    }
}
