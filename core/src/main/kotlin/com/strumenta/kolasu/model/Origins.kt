package com.strumenta.kolasu.model

import com.strumenta.kolasu.ast.Origin
import com.strumenta.kolasu.ast.Range

data class SimpleOrigin(
    override var range: Range?,
    override val sourceText: String? = null,
) : Origin

data class CompositeOrigin(
    val elements: List<Origin>,
    override var range: Range?,
    override val sourceText: String?,
) : Origin

data class NodeOrigin(
    val node: NodeLike,
) : Origin {
    override var range: Range?
        get() = node.range
        set(value) {
            node.range = value
        }
    override val sourceText: String?
        get() = node.sourceText
}
