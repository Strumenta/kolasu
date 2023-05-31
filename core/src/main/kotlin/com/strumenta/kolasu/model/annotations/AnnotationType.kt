package com.strumenta.kolasu.model.annotations

import com.strumenta.kolasu.model.Node

sealed class AnnotationType<I: AnnotationInstance>(val name: String, val multiple: Boolean) {

    val single: Boolean
        get() = !multiple
}

abstract class SingleAnnotationType<I: AnnotationInstance>(name: String) : AnnotationType<I>(name, false) {
    fun set(instance: AnnotationInstance) {
        TODO()
    }

    fun onNode(node: Node) : AnnotationInstance? {
        TODO()
    }
}

abstract class MultipleAnnotationType<I: AnnotationInstance>(name: String) : AnnotationType<I>(name, true) {

}