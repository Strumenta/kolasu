package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.ids.NodeIdProvider
import com.strumenta.kolasu.model.Node
import java.util.IdentityHashMap
import java.util.UUID

class UUIDNodeIdProvider : NodeIdProvider {
    private val cache = IdentityHashMap<KNode, String>()

    override fun id(kNode: Node): String =
        cache.getOrPut(kNode) {
            UUID.randomUUID().toString()
        }

    override var parentProvider: NodeIdProvider?
        get() = null
        set(value) {
            throw UnsupportedOperationException()
        }

    override fun registerMapping(
        kNode: Node,
        nodeId: String,
    ) {
        if (cache.containsKey(kNode)) {
            require(cache[kNode] == nodeId)
        } else {
            cache[kNode] = nodeId
        }
    }
}
