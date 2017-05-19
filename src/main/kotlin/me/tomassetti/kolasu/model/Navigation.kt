package me.tomassetti.kolasu.model

import java.util.*

fun Node.childParentMap() : Map<Node, Node> {
    val map = IdentityHashMap<Node, Node>()
    this.processConsideringParent({ child, parent -> if (parent != null) map[child] = parent })
    return map
}

fun <T: Node> Node.ancestor(klass: Class<T>, childParentMap: Map<Node, Node>) : T?{
    if (childParentMap.containsKey(this)) {
        val p = childParentMap[this]
        if (klass.isInstance(p)) {
            return p as T
        }
        return p!!.ancestor(klass, childParentMap)
    }
    return null
}

