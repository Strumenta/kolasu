package com.strumenta.kolasu.model

import kotlin.reflect.KClass

fun <I : AnnotationInstance> NodeLike.annotationsByType(kClass: KClass<I>): List<I> {
    return allAnnotationInstances.filterIsInstance(kClass.java)
}

fun <I : AnnotationInstance> NodeLike.getSingleAnnotation(kClass: KClass<I>): I? {
    val instances = allAnnotationInstances.filterIsInstance(kClass.java)
    return if (instances.isEmpty()) {
        null
    } else if (instances.size == 1) {
        instances.first()
    } else {
        throw IllegalStateException("More than one instance of $kClass found")
    }
}
