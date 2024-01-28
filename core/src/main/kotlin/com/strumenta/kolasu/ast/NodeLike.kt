package com.strumenta.kolasu.ast

import kotlin.reflect.KClass

fun <I : Annotation> NodeLike.annotationsByType(kClass: KClass<I>): List<I> {
    return allAnnotations.filterIsInstance(kClass.java)
}

fun <I : Annotation> NodeLike.getSingleAnnotation(kClass: KClass<I>): I? {
    val instances = allAnnotations.filterIsInstance(kClass.java)
    return if (instances.isEmpty()) {
        null
    } else if (instances.size == 1) {
        instances.first()
    } else {
        throw IllegalStateException("More than one instance of $kClass found")
    }
}
