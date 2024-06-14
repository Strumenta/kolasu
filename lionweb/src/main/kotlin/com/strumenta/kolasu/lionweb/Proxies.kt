package com.strumenta.kolasu.lionweb

data class ProxyNode(val nodeId: String) : KNode()

object ProxyBasedNodeResolver : NodeResolver {
    override fun resolve(nodeID: String): KNode? {
        return ProxyNode(nodeID)
    }
}
