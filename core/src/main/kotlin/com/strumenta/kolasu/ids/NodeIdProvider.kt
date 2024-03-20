package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.containingProperty
import com.strumenta.kolasu.model.Node as KNode

/**
 * This defines a policy to associate IDs to Kolasu Nodes.
 * This is necessary as Kolasu Nodes have no IDs, but several systems need IDs.
 * It is important that the logic is implemented so that given the same Node, the same ID is returned.
 */
interface NodeIdProvider {
    /**
     * @param coordinates the coordinates at which the node is to be placed
     * @return a valid LionWeb Node ID
     */
    fun idUsingCoordinates(kNode: KNode, coordinates: Coordinates): String

    fun id(kNode: KNode, overriddenCoordinates: Coordinates? = null): String {
        return idUsingCoordinates(kNode, overriddenCoordinates ?: calculatedCoordinates(kNode))
    }

    private fun calculatedCoordinates(kNode: Node): Coordinates {
        return if (kNode.parent == null) {
            RootCoordinates
        } else {
            NonRootCoordinates(this.id(kNode.parent!!), kNode.containingProperty()!!.name)
        }
    }
}

interface IDLogic {
    fun calculatedID(coordinates: Coordinates): String
}

sealed class Coordinates

object RootCoordinates : Coordinates()

data class NonRootCoordinates(
    val containerID: String,
    val containmentName: String
) : Coordinates()
