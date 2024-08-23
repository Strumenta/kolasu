package com.strumenta.kolasu.lionweb

import java.util.IdentityHashMap

/**
 * A thread-safe bidirectional map implementation
 *
 * @param A The type of the first set of elements
 * @param B Teh type of the second set of elements
 * @param usingIdentity Whether to use identity-based comparison for keys
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

    @Synchronized
    fun associate(
        a: A,
        b: B,
    ) {
        privateAsToBs[a] = b
        privateBsToAs[b] = a
    }

    @Synchronized
    fun byA(a: A): B? = privateAsToBs[a]

    @Synchronized
    fun byB(b: B): A? = privateBsToAs[b]

    @Synchronized
    fun containsA(a: A): Boolean = a in privateAsToBs

    @Synchronized
    fun containsB(b: B): Boolean = b in privateBsToAs

    @Synchronized
    fun clear() {
        privateAsToBs.clear()
        privateBsToAs.clear()
    }
}
