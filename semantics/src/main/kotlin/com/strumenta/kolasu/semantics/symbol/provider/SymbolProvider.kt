package com.strumenta.kolasu.semantics.symbol.provider

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.semantics.symbol.description.SymbolDescription

interface SymbolProvider {
    fun symbolFor(node: NodeLike): SymbolDescription?
}
