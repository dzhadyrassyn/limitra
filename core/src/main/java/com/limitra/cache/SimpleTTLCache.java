package com.limitra.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public class SimpleTTLCache<K, V> implements Cache<K, V> {

    TimeProvider time;
    ConcurrentHashMap<K, Entry<V>> map;
    LongAdder hits;
    LongAdder misses;
    LongAdder evictionsByTtl;
    LongAdder evictionsByCapacity;
    final int maxEntries;
    final LruList<K> lruList;

    public SimpleTTLCache(TimeProvider time) {
        this(time, Integer.MAX_VALUE);
    }

    public SimpleTTLCache(TimeProvider time, int maxEntries) {

        if (maxEntries <= 0) {
            throw new IllegalArgumentException("maxEntries must be greater than 0");
        }
        this.time = time;
        this.map = new ConcurrentHashMap<>();
        this.hits = new LongAdder();
        this.misses = new LongAdder();
        this.evictionsByTtl = new LongAdder();
        this.evictionsByCapacity = new LongAdder();
        this.maxEntries = maxEntries;
        this.lruList = new LruList<>();
    }

    @Override
    public void put(K key, V value) {
        internalPut(key, value, Long.MAX_VALUE);
    }

    private void internalPut(K key, V value, long ttlMillis) {

        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");

        lruList.recordAccess(key);
        map.put(key, new Entry<>(value, ttlMillis));

        while (map.size() > maxEntries) {
            K eldest = lruList.evictEldest();
            Entry<V> eldestItem = map.get(eldest);
            if (eldestItem != null && eldestItem.isExpired(time.nowNanos())) {
                if (remove(eldest)) {
                    evictionsByTtl.increment();
                }
            } else if (eldestItem != null) {
                if (remove(eldest)) {
                    evictionsByCapacity.increment();
                }
            }
        }
    }

    @Override
    public void put(K key, V value, long ttlMillis) {

        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("ttlMillis must be greater than 0");
        }
        internalPut(key, value, time.nowNanos() + TimeUnit.MILLISECONDS.toNanos(ttlMillis));
    }

    @Override
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key must not be null");

        Entry<V> entry = map.get(key);
        if (entry == null) {
            misses.increment();
            return Optional.empty();
        }

        if (entry.isExpired(time.nowNanos())) {
            misses.increment();
            evictionsByTtl.increment();
            remove(key);
            return Optional.empty();
        }

        hits.increment();
        lruList.recordAccess(key);
        return Optional.of(entry.value);
    }

    @Override
    public boolean remove(K key) {
        Objects.requireNonNull(key, "key must not be null");

        Entry<V> remove = map.remove(key);
        if (remove != null) {
            lruList.removeKey(key);
        }
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
                evictionsByTtl.increment();
                remove(key);
            } else {
                ++count;
            }
        }

        return count;
    }

    public CacheMetrics metricsSnapshot() {
        return new MetricsSnapshot(
                hits.sum(),
                misses.sum(),
                evictionsByTtl.sum(),
                evictionsByCapacity.sum()
        );
    }
}
