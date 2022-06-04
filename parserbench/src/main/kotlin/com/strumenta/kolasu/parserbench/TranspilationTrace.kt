package com.strumenta.kolasu.parserbench

import com.strumenta.kolasu.model.Node

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 */
class TranspilationTrace<S : Node, T : Node>(
    val originalCode: String,
    val sourceAST: S,
    val targetAST: T,
    val generatedCode: String
)
