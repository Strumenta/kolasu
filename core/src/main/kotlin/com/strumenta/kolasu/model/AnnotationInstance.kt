package com.strumenta.kolasu.model

open class AnnotationInstance(val type: AnnotationType<*>, val annotatedNode: Node) {
    init {
        annotatedNode.addAnnotation(this)
    }
}