package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node as KNode

/**
 * This defines a policy to associate IDs to Kolasu Nodes.
 * This is necessary as Kolasu Nodes have no IDs, but several systems need IDs.
 * It is important that the logic is implemented so that given the same Node, the same ID is returned.
 */
interface NodeIdProvider {
    /**
     * @return a valid LionWeb Node ID
     */
    fun id(kNode: KNode): String
}

interface IDLogic {
    val calculatedID: String
}
