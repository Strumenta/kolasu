package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.Node
import java.util.IdentityHashMap

/**
 * The Node ID of most nodes is calculated by considering the Node ID of the parent,
 * so that the Node IDs of all ancestors are calculated again and again.
 * By using a cache we can avoid that.
 */
class CachingNodeIDProvider(val wrapped: NodeIdProvider) : BaseNodeIdProvider() {
    private val cache = IdentityHashMap<Node, String>()

    override fun id(kNode: Node): String {
        if (cache.containsKey(kNode)) {
            return cache[kNode]!!
        } else {
            val value = wrapped.id(kNode)
            cache[kNode] = value
            return value
        }
    }

    init {
        wrapped.topLevelProvider.parentProvider = this
    }
}

fun NodeIdProvider.caching(): CachingNodeIDProvider = CachingNodeIDProvider(this)
