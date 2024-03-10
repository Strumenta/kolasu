package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals

class MySingleAnnotation : SimpleSingleAnnotation("MySingleAnnotation")

class MySimpleNode : MPNode()

class AnnotationTest {
    @Test
    fun attachAnnotation() {
        val n1 = MySimpleNode()
        val a1 = MySingleAnnotation()
        assertEquals(true, a1.isSingle)
        assertEquals(false, a1.isMultiple)

        assertEquals(false, n1.hasAnnotation(a1))
        assertEquals(false, a1.isAttached)
        assertEquals(null, a1.annotatedNode)
        assertEquals(emptyList(), n1.allAnnotations)
        assertEquals(emptyList(), n1.annotationsByType("MySingleAnnotation"))
        n1.addAnnotation(a1)
        assertEquals(listOf(a1), n1.allAnnotations)
        assertEquals(true, n1.hasAnnotation(a1))
        assertEquals(true, a1.isAttached)
        assertEquals(n1, a1.annotatedNode)
        assertEquals(listOf(a1), n1.annotationsByType("MySingleAnnotation"))
    }

    @Test
    fun detachAnnotation() {
        val n1 = MySimpleNode()
        val a1 = MySingleAnnotation()
        assertEquals(true, a1.isSingle)
        assertEquals(false, a1.isMultiple)

        n1.addAnnotation(a1)
        n1.removeAnnotation(a1)
        assertEquals(false, n1.hasAnnotation(a1))
        assertEquals(false, a1.isAttached)
        assertEquals(null, a1.annotatedNode)
        assertEquals(emptyList(), n1.allAnnotations)
        assertEquals(emptyList(), n1.annotationsByType("MySingleAnnotation"))
    }
}
