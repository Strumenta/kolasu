package com.strumenta.kolasu.model

/**
 * Note that type T is not strictly forced to be a Node. This is intended to support
 * interfaces like `Statement` or `Expression`. However, being an ancestor the returned
 * value is guaranteed to be a Node, as only Node instances can be part of the hierarchy.
 *
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T> Node.findAncestorOfType(klass: Class<T>): T? {
    return walkAncestors().filterIsInstance(klass).firstOrNull()
}
