package com.strumenta.kolasu.language

import kotlin.test.Test
import kotlin.test.assertEquals

class AnnotationTest {
    @Test
    fun annotationWithoutSuperAnnotation() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        assertEquals(null, a.superAnnotation)
        assertEquals(emptyList(), a.superClassifiers)
    }

    @Test
    fun annotationWithSuperAnnotation() {
        val l = StarLasuLanguage("my.language")
        val sa = Annotation(l, "SuperAnn")
        val a = Annotation(l, "Ann")
        a.superAnnotation = sa
        assertEquals(sa, a.superAnnotation)
        assertEquals(listOf(sa), a.superClassifiers)
    }
}
