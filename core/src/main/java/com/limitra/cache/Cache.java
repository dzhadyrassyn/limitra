package com.limitra.cache;

import java.util.Optional;

/**
 * A thread-safe in-memory cache API.
 *
 * <p>Contracts:
 * <ul>
 *   <li><b>Thread safety:</b> All operations are safe to invoke concurrently from multiple threads.
 *       Each individual method call is atomic with respect to internal state. However, the API does
 *       not guarantee compound atomicity across multiple calls (e.g., a "check then put" sequence
 *       is not atomic).</li>
 *   <li><b>Null policy:</b> All methods reject {@code null} keys and values. Passing {@code null}
 *       results in {@link NullPointerException}.</li>
 *   <li><b>Entry lifetime:</b> Entries may be <i>eternal</i> (no expiration) or <i>TTL-bound</i>
 *       (expire after a configured time-to-live). Once expired, an entry behaves as absent.</li>
 *   <li><b>Consistency:</b> Size and metrics values may be eventually consistent under concurrent
 *       access. The API does not define iteration or ordering guarantees.</li>
 * </ul>
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of values stored in this cache
 */
public interface Cache<K, V> {

    /**
     * Stores eternal entry (no TTL).
     * replaces the previous value and clears any prior TTL.
     * @param key of the entry
     * @param value of the entry
     * @throws NullPointerException for null key/value.
     */
    void put(K key, V value);

    /**
     * Stores value with time-to-live. Overwriting a key resets its TTL
     * @param key of the entry
     * @param value of the entry
     * @param ttlMillis time to expire of the entry, must be > 0. Resets TTL to the new value
     * @throws NullPointerException for null key/value.
     * @throws IllegalArgumentException if ttlMillis <= 0
     */
    void put(K key, V value, long ttlMillis);

    /**
     * Returns an Optional containing the value if present and not expired, otherwise Optional.empty(). Implementations may update hit/miss metrics
     * @param key of the entry
     * @return value by the key. Optional.empty() if absent or expired.
     * @throws NullPointerException for null key
     */
    Optional<V> get(K key);

    /**
     * true if something was removed.
     * @param key of the entry
     * @return true if an entry was removed, false otherwise.
     * @throws NullPointerException for null key
     */
    boolean remove(K key);

    /**
     * clear all entries in the cache
     */
    void clear();

    /**
     * size of non-expired & eventually consistent keys
     */
    long size();
}
