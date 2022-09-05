package com.strumenta.kolasu.model

import kotlin.reflect.KClass

interface Symbol : PossiblyNamed

class Scope private constructor(
    private val symbols: MutableMap<String, MutableList<Symbol>> = mutableMapOf(),
    private val parent: Scope? = null,
) {
    constructor(vararg symbols: Symbol, parent: Scope? = null) : this(symbols = listOf(*symbols), parent = parent)

    constructor(symbols: List<Symbol> = emptyList(), parent: Scope? = null) : this(
        symbols = symbols.groupBy { it.name ?: throw IllegalArgumentException("All given symbols must have a name") }
            .mapValues { it.value.toMutableList() }.toMutableMap(),
        parent = parent
    )

    fun add(symbol: Symbol) {
        val symbolName: String = symbol.name ?: throw IllegalArgumentException("The given symbol must have a name")
        this.symbols.computeIfAbsent(symbolName) { mutableListOf() }.add(symbol)
    }

    fun lookup(symbolName: String, symbolType: KClass<*> = Symbol::class): Symbol? {
        return this.symbols.getOrDefault(symbolName, mutableListOf()).find { symbolType.isInstance(it) }
            ?: this.parent?.lookup(symbolName, symbolType)
    }

    fun getSymbols(): Map<String, List<Symbol>> {
        return this.symbols
    }
}
