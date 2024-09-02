package com.strumenta.kolasu.model

actual class LRUCache<K, V> actual constructor(
    private val maxEntries: Int,
) : Map<K, V> {
    private val map: LinkedHashMap<K, V> =
        object : LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<K, V>): Boolean = size > maxEntries
        }

    override val entries: Set<Map.Entry<K, V>>
        get() = map.entries

    override val keys: Set<K>
        get() = map.keys

    override val size: Int
        get() = map.size

    override val values: Collection<V>
        get() = map.values

    override fun containsKey(key: K): Boolean = map.containsKey(key)

    override fun containsValue(value: V): Boolean = map.containsValue(value)

    override fun get(key: K): V? = map[key]

    override fun isEmpty(): Boolean = map.isEmpty()

    actual fun put(
        key: K,
        value: V,
    ) {
        map[key] = value
    }
}
