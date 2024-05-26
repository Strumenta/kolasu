package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Annotation

/**
 * Annotations are useful to attach additional data to nodes, without having to modify the node classes.
 * For example, things like comments, types, or other data calculated by processes successive to parsing
 * could be attached to a node through annotations.
 *
 * Each Node will be aware of the annotations attach to itself.
 */
abstract class AnnotationInstance {
    abstract val annotation: Annotation

    var annotatedNode: NodeLike? = null

    /**
     * Attach the annotation to a specific node.
     * The annotation should not be already attached to any node, not even to the same node to which we are attaching it.
     * This method is intended to be call by the node and not directly.
     */
    fun attachTo(node: NodeLike) {
        require(annotatedNode == null)
        annotatedNode = node
    }

    /**
     * The annotation will be detached, if attached to any node.
     * If the annotation was not attached to any node, nothing will happen.
     * This method is intended to be call by the node and not directly.
     */
    fun detach() {
        require(annotatedNode == null || !annotatedNode!!.hasAnnotation(this))
        annotatedNode = null
    }

    @Internal
    val isSingle: Boolean
        get() = annotation.isSingle

    @Internal
    val isMultiple: Boolean
        get() = annotation.isMultiple

    @Internal
    val isAttached: Boolean
        get() = annotatedNode?.hasAnnotation(this) ?: false
}

open class SimpleAnnotationInstance(
    override val annotation: Annotation,
) : AnnotationInstance()
