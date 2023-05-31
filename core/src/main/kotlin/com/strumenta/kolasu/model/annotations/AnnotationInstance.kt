package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Node

open class AnnotationInstance(val type: AnnotationType<*>, val annotatedNode: Node) {
    init {
        annotatedNode.addAnnotation(this)
    }
}