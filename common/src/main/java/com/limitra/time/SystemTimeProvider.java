package com.limitra.time;

/**
 * Production {@link TimeProvider} that uses {@link System#nanoTime()}.
 *
 * <p>Monotonic, not related to wall-clock time. Suitable for measuring elapsed durations (e.g.
 * cache TTLs, rate limiter refill).
 */
public class SystemTimeProvider implements TimeProvider {

    @Override
    public long nowNanos() {
        return System.nanoTime();
    }
}
