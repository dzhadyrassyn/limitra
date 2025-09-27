package com.limitra.cache;

import static org.junit.jupiter.api.Assertions.*;

import com.limitra.time.FakeTimeProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CacheTest {

    @Test
    void putThenGet_returnsValue() {

        // Given
        Cache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider());
        cache.put("key", 1);

        // When
        Optional<Integer> value = cache.get("key");

        // Then
        assertTrue(value.isPresent());
        assertEquals(1, value.get());
    }

    @Test
    void getAbsent_returnsEmpty() {

        // Given
        Cache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider());

        // When
        Optional<Integer> value = cache.get("key");

        // Then
        assertFalse(value.isPresent());
    }

    @Test
    void putWithTtl_expiresAfterDeadline() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        Cache<String, Integer> cache = new SimpleTTLCache<>(time);

        // When
        cache.put("a", 1, 50); // ms
        time.advanceMillis(60);

        // Then
        assertTrue(cache.get("a").isEmpty());
    }

    @Test
    void putEternal_thenPutWithTtl_replaceWithTtl() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        Cache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("a", 1);

        // When
        cache.put("a", 1, 100);
        time.advanceMillis(101);

        // Then
        assertTrue(cache.get("a").isEmpty());
    }

    @Test
    void putWithTtl_thenPutEternal_clearsTtl() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        Cache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("a", 1, 100);

        // When
        cache.put("a", 1);
        time.advanceMillis(101);

        // Then
        Optional<Integer> value = cache.get("a");
        assertTrue(value.isPresent());
        assertEquals(1, value.get());
    }

    @Test
    void removeExisting_returnsTrue_andKeyGone() {

        // Given
        Cache<String, Integer> cache = new SimpleTTLCache<>(new FakeTimeProvider());
        cache.put("a", 1);

        // When
        boolean removed = cache.remove("a");

        // Then
        assertTrue(removed);
        assertTrue(cache.get("a").isEmpty());
    }

    @Test
    void size_excludesExpiredKeys() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        Cache<String, Integer> cache = new SimpleTTLCache<>(time);
        cache.put("a", 1, 100);

        // When
        time.advanceMillis(101);

        // Then
        assertTrue(cache.get("a").isEmpty());
        assertEquals(0, cache.size());
    }
}
