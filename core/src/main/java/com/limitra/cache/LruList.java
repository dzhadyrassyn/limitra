package com.limitra.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Access-ordered LRU list for keys
 */
public class LruList<K> {

    private final LinkedHashMap<K, Void> lru;
    private final ReentrantLock lock;

    public LruList() {
        this.lru = new LinkedHashMap<>(16,0.75f, true);
        this.lock = new ReentrantLock();
    }

    /**
     * Creates or moves key to MRU position
     */
    public void recordAccess(K key) {
        lock.lock();
        try {
            lru.put(key, null);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove key from the LRU structure
     */
    public void removeKey(K key) {
        lock.lock();
        try {
            lru.remove(key);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Remove and return the least-recently-used key, or null if empty
     */
    public K evictEldest() {
        lock.lock();
        try {
            Iterator<K> it = lru.keySet().iterator();
            if (!it.hasNext()) {
                return null;
            }

            K eldest = it.next();
            it.remove();
            return eldest;
        } finally {
            lock.unlock();
        }
    }

    /**
     * For debugging/tests only
     */
    public int orderSize() {
        lock.lock();
        try {
            return lru.size();
        } finally {
            lock.unlock();
        }
    }

}
