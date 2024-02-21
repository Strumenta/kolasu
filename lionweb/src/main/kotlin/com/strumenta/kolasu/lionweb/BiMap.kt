package com.strumenta.kolasu.lionweb

import java.util.IdentityHashMap

class BiMap<A, B>(val usingIdentity: Boolean = false) {
    val `as`: Set<A>
        get() = _asToBs.keys
    val bs: Set<B>
        get() = _bsToAs.keys
    val asToBsMap: Map<A, B>
        get() = _asToBs
    val bsToAsMap: Map<B, A>
        get() = _bsToAs

    private val _asToBs = if (usingIdentity) IdentityHashMap() else mutableMapOf<A, B>()
    private val _bsToAs = if (usingIdentity) IdentityHashMap() else mutableMapOf<B, A>()

    fun associate(a: A, b: B) {
        _asToBs[a] = b
        _bsToAs[b] = a
    }

    fun byA(a: A): B? = _asToBs[a]
    fun byB(b: B): A? = _bsToAs[b]

    fun containsA(a: A): Boolean = _asToBs.containsKey(a)
    fun containsB(b: B): Boolean = _bsToAs.containsKey(b)
    fun clear() {
        _asToBs.clear()
        _bsToAs.clear()
    }
}
