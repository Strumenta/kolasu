package com.strumenta.kolasu.model

/**
 * @return the nearest ancestor of this node that is an instance of klass.
 */
fun <T : Node> Node.findAncestorOfType(klass: Class<T>): T? {
    return walkAncestors().filterIsInstance(klass).firstOrNull()
}
