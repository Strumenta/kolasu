package com.strumenta.kolasu.model

@Suppress("UNCHECKED_CAST")
fun <T : Node> Node.ancestor(klass: Class<T>): T? {
    if (this.parent != null) {
        if (klass.isInstance(this.parent)) {
            return this.parent as T
        }
        return this.parent!!.ancestor(klass)
    }
    return null
}
