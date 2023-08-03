package com.strumenta.kolasu.lionweb

class BiMap<A, B> {
    val `as`: Set<A>
        get() = _asToBs.keys
    val bs: Set<B>
        get() = _bsToAs.keys
    val asToBsMap: Map<A, B>
        get() = _asToBs
    val bsToAsMap: Map<B, A>
        get() = _bsToAs

    private val _asToBs = mutableMapOf<A, B>()
    private val _bsToAs = mutableMapOf<B, A>()

    fun associate(a: A, b: B) {
        _asToBs[a] = b
        _bsToAs[b] = a
    }

    fun byA(a: A): B? = _asToBs[a]
    fun byB(b: B): A? = _bsToAs[b]
}
