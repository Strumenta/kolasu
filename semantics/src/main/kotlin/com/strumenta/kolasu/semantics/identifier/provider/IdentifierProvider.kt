package com.strumenta.kolasu.semantics.identifier.provider

import com.strumenta.kolasu.model.Node
import kotlin.reflect.KClass

interface IdentifierProvider {
    /**
     * Retrieve the identifier associated
     * with the given node (possibly cast as requested)
     **/
    fun <NodeTy : Node> getIdentifierFor(
        node: NodeTy,
        typedAs: KClass<in NodeTy>? = null
    ): String?
}
