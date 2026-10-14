package com.abhiram.music.utils

/**
 * A tiny fixed-capacity, access-ordered key/value buffer.
 *
 * The playback pipeline keeps at most [DEFAULT_CAPACITY] freshly resolved stream URLs in memory
 * (see `com.abhiram.music.service.ResolvingDataSourceFactory`) so that opening the next chunk of
 * the previous/next track never has to hit InnerTube again. All accessors are `synchronized`, so
 * the buffer is safe to share between the loader threads that resolve streams and the main thread
 * that drives the player.
 *
 * The buffer evicts the least-recently-accessed entry once [capacity] is exceeded, which keeps the
 * two most recent working sets (previous + next track) resident while every older URL drops out.
 *
 * ```kotlin
 * val buffer = RingBuffer<String, String>()   // capacity = 2
 * buffer.put("dQw4w9WgXcQ", "https://…")
 * buffer["dQw4w9WgXcQ"]                       // refreshes recency
 * ```
 *
 * @param K key type, e.g. a YouTube video id.
 * @param V value type, e.g. a resolved stream URL.
 * @param capacity maximum number of entries retained; the oldest access is evicted beyond it.
 */
class RingBuffer<K : Any, V : Any>(private val capacity: Int = DEFAULT_CAPACITY) {

    init {
        require(capacity > 0) { "RingBuffer capacity must be greater than zero" }
    }

    /** Access-ordered map; `get` counts as an access so recency stays true LRU-ish. */
    private val entries = object : LinkedHashMap<K, V>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > capacity
    }

    /** Number of entries currently held, between `0` and [capacity]. */
    val size: Int
        get() = synchronized(entries) { entries.size }

    /** Returns the value for [key], or `null`. A successful lookup refreshes recency. */
    operator fun get(key: K): V? = synchronized(entries) { entries[key] }

    /** Inserts or replaces [key]; evicts the least-recently-accessed entry when over capacity. */
    fun put(key: K, value: V) {
        synchronized(entries) { entries[key] = value }
    }

    /** Removes [key], returning the previous value if it was present. */
    fun remove(key: K): V? = synchronized(entries) { entries.remove(key) }

    /** Returns `true` when [key] is currently buffered. */
    fun contains(key: K): Boolean = synchronized(entries) { entries.containsKey(key) }

    /** Snapshot of the buffered keys, oldest access first. */
    fun keys(): List<K> = synchronized(entries) { entries.keys.toList() }

    /** Drops every entry. */
    fun clear() {
        synchronized(entries) { entries.clear() }
    }

    companion object {
        /** Default fixed capacity: the two most recent (videoId → url) pairs. */
        const val DEFAULT_CAPACITY: Int = 2
    }
}
