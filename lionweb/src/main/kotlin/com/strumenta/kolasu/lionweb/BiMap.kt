package com.strumenta.kolasu.lionweb

import java.util.IdentityHashMap

class BiMap<A, B>(
    val usingIdentity: Boolean = false,
) {
    val `as`: Set<A>
        get() = privateAsToBs.keys
    val bs: Set<B>
        get() = privateBsToAs.keys
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
        privateAsToBs[a] = b
        privateBsToAs[b] = a
    }

    fun byA(a: A): B? = privateAsToBs[a]

    fun byB(b: B): A? = privateBsToAs[b]

    fun containsA(a: A): Boolean = privateAsToBs.containsKey(a)

    fun containsB(b: B): Boolean = privateBsToAs.containsKey(b)
}
