package com.strumenta.kolasu.model

import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.annotations.SingleAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

data class DocumentationAnnotationInstance(val text: String)
    : SingleAnnotation()

class N1 : Node()

class AnnotationsTest {

    @Test
    fun reportText() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(DocumentationAnnotationInstance("Hello A"))

        assertEquals(true, ann1.attached)
        assertEquals(a, ann1.annotatedNode)
        assertEquals(listOf(ann1), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann1), a.getAnnotations(Annotation::class))
        assertEquals(listOf(),  b.getAnnotations(Annotation::class))
        a.removeAnnotation(ann1)
        assertEquals(null, ann1.annotatedNode)
        assertEquals(false, ann1.attached)
        assertEquals(listOf(), a.getAnnotations())
        assertEquals(listOf(), a.getAnnotations(Annotation::class))


        val ann2 = a.addAnnotation(DocumentationAnnotationInstance("Hello A, again"))
        assertEquals(true, ann2.attached)
        val ann3 = a.addAnnotation(DocumentationAnnotationInstance("Hello A, third time is a charm"))
        assertEquals(false, ann2.attached)
        assertEquals(null, ann2.annotatedNode)
        assertEquals(true, ann3.attached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann3), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann3), a.getAnnotations(Annotation::class))
        assertEquals(listOf(),  b.getAnnotations(Annotation::class))

    }

}
