package com.limitra.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketRateLimiterTest {

    @Test
    void newLimiter_startsFull_allowsBurstUpToCapacity() {

        // Given
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(new FakeTimeProvider(), 5L, 2.0);

        // When Then
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }

        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void steadyRefill_refillsOverTime() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, 5L, 2.0);
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryAcquire();
        }

        // When
        time.advanceMillis(500);

        // Then
        assertTrue(rateLimiter.tryAcquire());
        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void idleRefills_upToCapacity_notBeyond() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        long capacity = 5L;
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, 2.0);

        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire();
        }

        // When
        time.advanceSeconds(10);

        // Then
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void acquireMultiplePermits_succeedsWhenEnoughTokens() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, 5L, 2.0);

        // When Then
        assertTrue(rateLimiter.tryAcquire(3));
        assertFalse(rateLimiter.tryAcquire(3));

        time.advanceSeconds(1);
        assertTrue(rateLimiter.tryAcquire(2));
    }

    @Test
    void acquireMoreThanCapacity_neverSucceeds() {

        // Given
        long capacity = 5L;
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(new FakeTimeProvider(), capacity, 10);

        // When Then
        assertFalse(rateLimiter.tryAcquire((int)capacity + 1));
    }

    @Test
    void fractionalRate_refillsAccurately() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        long capacity = 5L;
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, 0.5);

        // When
        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire();
        }

        // Then
        assertFalse(rateLimiter.tryAcquire());
        time.advanceSeconds(2);
        assertTrue(rateLimiter.tryAcquire());

        time.advanceSeconds(1);
        assertFalse(rateLimiter.tryAcquire());

        time.advanceSeconds(1);
        assertTrue(rateLimiter.tryAcquire());
    }

    @Test
    void refill_isCappedAtCapacity_afterLongIdle() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        long capacity = 3L;
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, 10);

        for (int i = 0; i < capacity; i++) {
            rateLimiter.tryAcquire();
        }

        // When
        time.advanceSeconds(60);

        // Then
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }

        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void constructor_invalidArgs_throw() {

        assertThrows(IllegalArgumentException.class, () -> new TokenBucketRateLimiter(new FakeTimeProvider(), 0, 0));
    }

    @Test
    void tryAcquire_invalidPermits_throw() {

        // Given
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(new FakeTimeProvider(), 1L, 0.5);

        // When, Then
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire(0));
        assertThrows(IllegalArgumentException.class, () -> rateLimiter.tryAcquire(-1));
    }

    @Test
    void zeroRate_neverRefills() {

        // Given
        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, 2L, 0);

        // When
        assertTrue(rateLimiter.tryAcquire());
        assertTrue(rateLimiter.tryAcquire());
        time.advanceSeconds(10);

        // Then
        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void noTimeAdvance_noRefillBetweenBackToBackCalls() {

        // Given
        long capacity = 2L;
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(new FakeTimeProvider(), capacity, 100);

        // When, Then
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        assertFalse(rateLimiter.tryAcquire());
    }
}
