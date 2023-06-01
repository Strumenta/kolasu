package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Internal
import com.strumenta.kolasu.model.Node

sealed class Annotation {
    fun attachTo(node: Node) {
        require(annotatedNode == null)
        annotatedNode = node
    }

    fun detach() {
        annotatedNode = null
    }

    var annotatedNode: Node? = null

    constructor() {

    }
    @Internal
    val annotationType: String
        get() = this::class.qualifiedName!!

    @Internal
    val single : Boolean
        get() = !multiple

    @Internal
    abstract val multiple : Boolean

    @Internal
    val attached: Boolean
        get() = annotatedNode?.hasAnnotation(this) ?: false
}

abstract class SingleAnnotation() : Annotation() {
    override val multiple: Boolean
        get() = false
}

abstract class MultipleAnnotation() : Annotation() {
    override val multiple: Boolean
        get() = true
}