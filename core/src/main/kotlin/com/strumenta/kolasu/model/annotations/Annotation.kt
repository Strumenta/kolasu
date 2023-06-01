package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Internal
import com.strumenta.kolasu.model.Node

sealed class Annotation(open val annotatedNode: Node) {
    init {
        this.annotatedNode.addAnnotation(this)
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
    var attached: Boolean = false
}

abstract class SingleAnnotation(annotatedNode: Node) : Annotation(annotatedNode) {
    override val multiple: Boolean
        get() = false
}

abstract class MultipleAnnotation(annotatedNode: Node) : Annotation(annotatedNode) {
    override val multiple: Boolean
        get() = true
}