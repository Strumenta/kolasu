package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Internal
import com.strumenta.kolasu.model.Node

/**
 * Annotations are useful to attach additional data to nodes, without having to modify the node classes.
 * For example, things like comments, types, or other data calculated by processes successive to parsing
 * could be attached to a node through annotations.
 *
 * Each Node will be aware of the annotations attach to itself.
 */
sealed class Annotation {
    var annotatedNode: Node? = null

    /**
     * Attach the annotation to a specific node.
     * The annotation should not be already attached to any node, not even to the same node to which we are attaching it.
     */
    fun attachTo(node: Node) {
        require(annotatedNode == null)
        annotatedNode = node
    }

    /**
     * The annotation will be detached, if attached to any node.
     * If the annotation was not attached to any node, nothing will happen.
     */
    fun detach() {
        require(annotatedNode == null || !annotatedNode!!.hasAnnotation(this))
        annotatedNode = null
    }

    @Internal
    val annotationType: String
        get() = this::class.qualifiedName!!

    @Internal
    val single: Boolean
        get() = !multiple

    @Internal
    abstract val multiple: Boolean

    @Internal
    val attached: Boolean
        get() = annotatedNode?.hasAnnotation(this) ?: false
}

/**
 * A Single Annotation. Each Node can have at most one instance attached for each type of SingleAnnotation.
 */
abstract class SingleAnnotation : Annotation() {
    override val multiple: Boolean
        get() = false
}

/**
 * A Multiple Annotation. Each Node can have any number of instances attached for each type of MultipleAnnotation.
 */
abstract class MultipleAnnotation : Annotation() {
    override val multiple: Boolean
        get() = true
}
