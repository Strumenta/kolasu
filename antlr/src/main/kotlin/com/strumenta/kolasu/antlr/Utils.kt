package com.strumenta.kolasu.antlr

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeDestination
import com.strumenta.kolasu.model.SimpleOrigin

fun Node.detachFromParseTree(keepRange: Boolean = true, keepSourceText: Boolean = false) {
    val existingOrigin = origin
    if (existingOrigin != null) {
        if (keepRange || keepSourceText) {
            this.origin = SimpleOrigin(
                if (keepRange) existingOrigin.range else null,
                if (keepSourceText) existingOrigin.sourceText else null
            )
        } else {
            this.origin = null
        }
        if (existingOrigin is Node && existingOrigin.destinations.contains(NodeDestination(this))) {
            existingOrigin.destinations.remove(NodeDestination(this))
        }
    }
}
