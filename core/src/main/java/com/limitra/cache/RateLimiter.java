package com.limitra.cache;

/**
 * A thread-safe rate limiter that controls how many permits (tokens) can be acquired over time.
 * Implementations typically enforce a maximum capacity (burst size) and a steady refill rate.
 *
 * <p>The canonical implementation is a token bucket: the bucket starts full, refills at a fixed
 * rate, and each call to {@code tryAcquire(...)} consumes permits if available. Calls that cannot
 * be satisfied immediately return {@code false}.
 *
 * <h3>Concurrency</h3>
 *
 * <ul>
 *   <li>All methods are safe under concurrent access.
 *   <li>No fairness guarantees are provided: concurrent threads race to acquire tokens.
 * </ul>
 *
 * <h3>Usage</h3>
 *
 * <pre>{@code
 * RateLimiter limiter = new TokenBucketRateLimiter(
 *     new SystemTimeProvider(),
 *     5,    // capacity
 *     2.0   // permits per second
 * );
 *
 * if (limiter.tryAcquire()) {
 *     // allowed
 * } else {
 *     // rejected
 * }
 * }</pre>
 */
public interface RateLimiter {

    /**
     * Attempts to acquire a single permit.
     *
     * @return {@code true} if a permit was acquired, {@code false} if the request is rate-limited
     */
    boolean tryAcquire();

    /**
     * Attempts to acquire the given number of permits.
     *
     * @param permits number of permits to consume; must be greater than 0
     * @return {@code true} if the permits were acquired, {@code false} if not enough were available
     * @throws IllegalArgumentException if {@code permits <= 0}
     */
    boolean tryAcquire(int permits);
}
