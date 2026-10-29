package com.markethub.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Small thread-safe "least recently used" cache.
 *
 * <p>Built on {@link LinkedHashMap} in access-order mode: every read moves the entry to the
 * back of the list, and {@code removeEldestEntry} evicts the front entry once the cache is
 * full. {@code LinkedHashMap} itself is not thread-safe, so each public method is
 * {@code synchronized}.</p>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LruCache<K, V> {

    private final Map<K, V> entries;

    public LruCache(final int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.entries = new LinkedHashMap<K, V>(16, 0.75f, true) {
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public synchronized Optional<V> find(K key) {
        return Optional.ofNullable(entries.get(key));
    }

    public synchronized void put(K key, V value) {
        entries.put(key, value);
    }

    public synchronized void remove(K key) {
        entries.remove(key);
    }

    public synchronized void clear() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }
}
