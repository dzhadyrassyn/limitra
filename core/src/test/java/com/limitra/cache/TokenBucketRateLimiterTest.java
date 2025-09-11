package com.limitra.cache;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TokenBucketRateLimiterTest {

    @Test
    void newLimiter_startsFull_allowsBurstUpToCapacity() {

        // Given
        TokenBucketRateLimiter rateLimiter =
                new TokenBucketRateLimiter(new FakeTimeProvider(), 5L, 2.0);

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
        TokenBucketRateLimiter rateLimiter =
                new TokenBucketRateLimiter(new FakeTimeProvider(), capacity, 10);

        // When Then
        assertFalse(rateLimiter.tryAcquire((int) capacity + 1));
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

        assertThrows(
                IllegalArgumentException.class,
                () -> new TokenBucketRateLimiter(new FakeTimeProvider(), 0, 0));
    }

    @Test
    void tryAcquire_invalidPermits_throw() {

        // Given
        TokenBucketRateLimiter rateLimiter =
                new TokenBucketRateLimiter(new FakeTimeProvider(), 1L, 0.5);

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
        TokenBucketRateLimiter rateLimiter =
                new TokenBucketRateLimiter(new FakeTimeProvider(), capacity, 100);

        // When, Then
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void concurrent_tryAcquire_doesNotOverSpend_andNoExceptions() throws InterruptedException {

        // Given
        long capacity = 20L;
        TokenBucketRateLimiter rateLimiter =
                new TokenBucketRateLimiter(new FakeTimeProvider(), 20L, 0);
        AtomicInteger count = new AtomicInteger();

        Runnable task =
                () -> {
                    for (int i = 0; i < 1_000; i++) {
                        if (rateLimiter.tryAcquire()) {
                            count.incrementAndGet();
                        }
                    }
                };

        Thread thread1 = new Thread(task);
        Thread thread2 = new Thread(task);
        Thread thread3 = new Thread(task);
        Thread thread4 = new Thread(task);

        // When
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();

        // Then
        assertTrue(count.get() <= capacity);
    }

    @Test
    void concurrent_mixedAcquireAndRefill_capacityNeverExceeded() throws InterruptedException {

        // Given
        long capacity = 64L;
        double rate = 10.0;
        long durationMillis = 3_000;
        int threads = 4;

        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, rate);

        AtomicInteger successes = new AtomicInteger();
        AtomicReference<Throwable> error = new AtomicReference<>();
        AtomicBoolean running = new AtomicBoolean(true);

        Thread refiller =
                new Thread(
                        () -> {
                            try {
                                long advanced = 0;
                                while (running.get() && advanced < durationMillis) {
                                    time.advanceMillis(100);
                                    advanced += 100;
                                }
                            } catch (Throwable t) {
                                error.compareAndSet(null, t);
                            } finally {
                                running.set(false);
                            }
                        },
                        "refiller thread");

        Runnable worker =
                () -> {
                    try {
                        ThreadLocalRandom rnd = ThreadLocalRandom.current();
                        while (running.get()) {
                            int permits = (rnd.nextInt(100) < 30) ? 2 : 1; // ~30%
                            if (rateLimiter.tryAcquire(permits)) {
                                successes.addAndGet(permits);
                            }
                        }
                    } catch (Throwable t) {
                        error.compareAndSet(null, t);
                    }
                };

        refiller.start();
        Thread[] workers = new Thread[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new Thread(worker, "worker-" + i);
            workers[i].start();
        }

        refiller.join();
        for (Thread w : workers) w.join();

        assertNull(error.get(), "Background thread failed: " + error.get());

        double seconds = durationMillis / 1000.0;
        double theoretical = capacity + rate * seconds;
        int observed = successes.get();

        int upperBound = (int) Math.ceil(theoretical * 1.2);
        assertTrue(observed <= upperBound, "Observed " + observed + " > loose bound " + upperBound);
    }

    @Test
    void refillUsesMonotonicTime_notWallClock() {

        // Given
        long capacity = 5L;
        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, 2);

        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }

        assertFalse(rateLimiter.tryAcquire());
        assertFalse(rateLimiter.tryAcquire());

        // When
        time.advanceSeconds(1);

        // Then
        assertTrue(rateLimiter.tryAcquire());
        assertTrue(rateLimiter.tryAcquire());
        assertFalse(rateLimiter.tryAcquire());
    }

    @Test
    void largeElapsedTime_singleRefillStep_capsCorrectly() {

        // Given
        long capacity = 5L;
        FakeTimeProvider time = new FakeTimeProvider();
        TokenBucketRateLimiter rateLimiter = new TokenBucketRateLimiter(time, capacity, 2);

        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }

        // When
        time.advanceSeconds(3600);

        // Then
        for (int i = 0; i < capacity; i++) {
            assertTrue(rateLimiter.tryAcquire());
        }
        assertFalse(rateLimiter.tryAcquire());
    }
}
