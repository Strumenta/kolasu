package com.strumenta.kolasu.semantics.symbol.repository

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription
import kotlin.reflect.KClass

interface SymbolRepository {
    fun load(identifier: String): SymbolDescription?

    fun store(symbol: SymbolDescription)

    fun find(withType: KClass<out Node>): Sequence<SymbolDescription>
}
