package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Node

sealed class AnnotationInstance(val annotatedNode: Node) {
    init {
        annotatedNode.addAnnotation(this)
    }
}

abstract class SingleAnnotationInstance(annotatedNode: Node) : AnnotationInstance(annotatedNode){

}

abstract class MultipleAnnotationInstance(annotatedNode: Node) : AnnotationInstance(annotatedNode){

}