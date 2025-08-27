package com.limitra.cache;

public final class Entry<V> {

    final V value;
    final long expiresAtNanos;

    public Entry(V value, long expiresAtNanos) {
        this.value = value;
        this.expiresAtNanos = expiresAtNanos;
    }

    boolean isExpired(long now) {
        return now >= expiresAtNanos;
    }
}
