package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription

interface SymbolProvider {
    fun symbolFor(node: Node?): SymbolDescription?
}
