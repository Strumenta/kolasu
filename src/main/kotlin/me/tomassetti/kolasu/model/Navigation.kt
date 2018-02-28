package me.tomassetti.kolasu.model

import java.util.*

//typealias ChildParentMap = Map<Node, Node>
//
///**
// * To be invoked on the root of an AST. It finds and collect all the node -> parent
// * relationships. Each Node knows its children but the opposite is not true.
// * Through ChildrenParentMaps the children can access the parents.
// */
//fun Node.childParentMap() : ChildParentMap {
//    val map = IdentityHashMap<Node, Node>()
//    this.processConsideringParent({ child, parent -> if (parent != null) map[child] = parent })
//    return map
//}

fun <T: Node> Node.ancestor(klass: Class<T>) : T?{
    if (this.parent != null) {
        if (klass.isInstance(this.parent)) {
            return this.parent as T
        }
        return this.parent!!.ancestor(klass)
    }
    return null
}
