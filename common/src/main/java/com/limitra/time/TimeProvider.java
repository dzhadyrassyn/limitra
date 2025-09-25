package com.limitra.time;

public interface TimeProvider {

    /**
     * @implNote Implementations must be monotonic; wall-clock is not used for expiry decisions.
     * @return the current time in nanoseconds, suitable for measuring durations
     */
    long nowNanos();
}
