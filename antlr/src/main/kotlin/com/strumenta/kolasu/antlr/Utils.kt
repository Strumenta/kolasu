package com.strumenta.kolasu.antlr

import com.strumenta.kolasu.antlr.parsing.ParseTreeOrigin
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.SimpleOrigin

/**
 * Remove links to the ParseTree, in order to save memory.
 */
fun Node.detachFromParseTree(keepRange: Boolean = true, keepSourceText: Boolean = false) {
    val existingOrigin = origin
    if (existingOrigin is ParseTreeOrigin) {
        if (keepRange || keepSourceText) {
            this.origin = SimpleOrigin(
                if (keepRange) existingOrigin.range else null,
                if (keepSourceText) existingOrigin.sourceText else null
            )
        } else {
            this.origin = null
        }
    }
}
