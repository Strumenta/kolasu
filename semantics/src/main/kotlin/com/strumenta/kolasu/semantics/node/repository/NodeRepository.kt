package com.strumenta.kolasu.semantics.node.repository

import com.strumenta.kolasu.model.Node

interface NodeRepository {
    fun load(identifier: String): Node?

    fun store(node: Node): String?
}
