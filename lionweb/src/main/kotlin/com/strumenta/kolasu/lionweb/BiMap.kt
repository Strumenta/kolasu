package com.strumenta.kolasu.lionweb

class BiMap<A, B> {
    val `as`: Set<A>
        get() = privateAsToBs.keys
    val bs: Set<B>
        get() = privateBsToAs.keys
    val asToBsMap: Map<A, B>
        get() = privateAsToBs
    val bsToAsMap: Map<B, A>
        get() = privateBsToAs

    private val privateAsToBs = mutableMapOf<A, B>()
    private val privateBsToAs = mutableMapOf<B, A>()

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
