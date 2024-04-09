package com.strumenta.kolasu.lionweb

import java.util.IdentityHashMap

/**
 * This class is thread-safe.
 */
class BiMap<A, B>(
    val usingIdentity: Boolean = false,
) {
    val `as`: Set<A>
        get() = synchronized(this) { privateAsToBs.keys }
    val bs: Set<B>
        get() = synchronized(this) { privateBsToAs.keys }
    val asToBsMap: Map<A, B>
        get() = privateAsToBs
    val bsToAsMap: Map<B, A>
        get() = privateBsToAs

    private val privateAsToBs = if (usingIdentity) IdentityHashMap() else mutableMapOf<A, B>()
    private val privateBsToAs = if (usingIdentity) IdentityHashMap() else mutableMapOf<B, A>()

    fun associate(
        a: A,
        b: B,
    ) {
        synchronized(this) {
            privateAsToBs[a] = b
            privateBsToAs[b] = a
        }
    }

    fun byA(a: A): B? = synchronized(this) { privateAsToBs[a] }

    fun byB(b: B): A? = synchronized(this) { privateBsToAs[b] }

    fun containsA(a: A): Boolean = synchronized(this) { privateAsToBs.containsKey(a) }

    fun containsB(b: B): Boolean = synchronized(this) { privateBsToAs.containsKey(b) }

    fun clear() {
        synchronized(this) {
            privateAsToBs.clear()
            privateBsToAs.clear()
        }
    }
}
