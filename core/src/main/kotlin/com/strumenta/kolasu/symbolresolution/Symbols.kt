package com.strumenta.kolasu.symbolresolution

import com.strumenta.kolasu.model.PossiblyNamed

interface Symbol : PossiblyNamed
typealias SymbolTable = MutableMap<String, MutableList<Symbol>>
