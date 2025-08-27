package com.limitra.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class SimpleTTLCache<K, V> implements Cache<K, V> {

    TimeProvider time;
    ConcurrentHashMap<K, Entry<V>> map;

    public SimpleTTLCache(TimeProvider time) {
        this.time = time;
        this.map = new ConcurrentHashMap<>();
    }

    @Override
    public void put(K key, V value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        map.put(key, new Entry<>(value, Long.MAX_VALUE));

    }

    @Override
    public void put(K key, V value, long ttlMillis) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be greater than 0");
        }

        long now = time.nowNanos();
        map.put(key, new Entry<>(value, now + TimeUnit.MILLISECONDS.toNanos(ttlMillis)));
    }

    @Override
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");

        Entry<V> entry = map.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        if (entry.isExpired(time.nowNanos())) {
            map.remove(key);
            return Optional.empty();
        }

        return Optional.of(entry.value);
    }

    @Override
    public boolean remove(K key) {
        Objects.requireNonNull(key, "key must not be null");

        Entry<V> remove = map.remove(key);
        return remove != null;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public long size() {
        //TODO Optimize later this O(n) implementation
        long count = 0;
        for (K key : map.keySet()) {
            if (map.get(key).isExpired(time.nowNanos())) {
                remove(key);
            } else {
                ++count;
            }
        }

        return count;
    }
}
