package com.strumenta.kolasu.model

interface Origin {
    var range: Range?
    val sourceText: String?
    val source: Source?
        get() = range?.source
}

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

fun <N : NodeLike> N.withOrigin(node: NodeLike): N {
    this.origin =
        if (node == this) {
            null
        } else {
            NodeOrigin(node)
        }
    return this
}

fun <N : NodeLike> N.withOrigin(origin: Origin?): N {
    this.origin =
        if (origin == NodeOrigin(this)) {
            null
        } else {
            origin
        }
    return this
}
