package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.NodeLike as KNode

/**
 * This defines a policy to associate IDs to Kolasu Nodes.
 * This is necessary as Kolasu Nodes have no IDs, but several systems need IDs.
 * It is important that the logic is implemented so that given the same Node, the same ID is returned.
 */
interface NodeIdProvider {
    fun id(kNode: KNode): String

    var parentProvider: NodeIdProvider?
    val topLevelProvider: NodeIdProvider
        get() = if (parentProvider == null) this else parentProvider!!.topLevelProvider
}

abstract class BaseNodeIdProvider : NodeIdProvider {
    override var parentProvider: NodeIdProvider? = null
        set(value) {
            field =
                if (value == this) {
                    null
                } else {
                    value
                }
        }
}

/**
 * The common approach for calculating Node IDs is to use Semantic Node IDs where present,
 * and Positional Node IDs in the other cases.
 */
class CommonNodeIdProvider(
    val semanticIDProvider: SemanticNodeIDProvider = DeclarativeNodeIdProvider(),
) : BaseNodeIdProvider() {
    override fun id(kNode: NodeLike): String =
        if (semanticIDProvider.hasSemanticIdentity(kNode)) {
            semanticIDProvider.semanticID(kNode)
        } else {
            positionalID(kNode)
        }

    private fun positionalID(kNode: NodeLike): String =
        StructuralNodeIdProvider().apply { parentProvider = this }.id(
            kNode,
        )
}

interface SemanticNodeIDProvider {
    fun hasSemanticIdentity(kNode: NodeLike): Boolean

    fun semanticID(kNode: NodeLike): String
}
