package com.strumenta.kolasu.antlr4k

import com.strumenta.kolasu.antlr4k.parsing.ParseTreeOrigin
import com.strumenta.kolasu.ast.NodeLike
import com.strumenta.kolasu.ast.SimpleOrigin

/**
 * Remove links to the ParseTree, in order to save memory.
 */
fun NodeLike.detachFromParseTree(
    keepRange: Boolean = true,
    keepSourceText: Boolean = false,
) {
    val existingOrigin = origin
    if (existingOrigin is ParseTreeOrigin) {
        if (keepRange || keepSourceText) {
            this.origin =
                SimpleOrigin(
                    if (keepRange) existingOrigin.range else null,
                    if (keepSourceText) existingOrigin.sourceText else null,
                )
        } else {
            this.origin = null
        }
    }
}
