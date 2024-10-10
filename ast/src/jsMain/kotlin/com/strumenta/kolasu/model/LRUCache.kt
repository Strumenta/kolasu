package com.strumenta.kolasu.model

actual class LRUCache<K, V> actual constructor(
    private val maxEntries: Int,
) : Map<K, V> {
    private val map = mutableMapOf<K, V>()
    private val order = mutableListOf<K>()

    actual override val entries: Set<Map.Entry<K, V>>
        get() = map.entries

    actual override val keys: Set<K>
        get() = map.keys

    actual override val size: Int
        get() = map.size

    actual override val values: Collection<V>
        get() = map.values

    actual override fun containsKey(key: K): Boolean = map.containsKey(key)

    actual override fun containsValue(value: V): Boolean = map.containsValue(value)

    actual override fun get(key: K): V? =
        map[key]?.also {
            // Move the accessed key to the end of the list
            order.remove(key)
            order.add(key)
        }

    actual override fun isEmpty(): Boolean = map.isEmpty()

    actual fun put(
        key: K,
        value: V,
    ) {
        if (map.containsKey(key)) {
            // If the key exists, move it to the end of the list
            order.remove(key)
        } else if (map.size >= maxEntries) {
            // Remove the least recently used entry
            val oldestKey = order.removeAt(0)
            map.remove(oldestKey)
        }
        order.add(key)
        map[key] = value
    }
}
