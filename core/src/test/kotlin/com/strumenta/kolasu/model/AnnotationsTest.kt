package com.strumenta.kolasu.model

import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.annotations.SingleAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

data class DocumentationAnnotationInstance(override val annotatedNode: Node, val text: String)
    : SingleAnnotation(annotatedNode)

class N1 : Node()

class AnnotationsTest {

    @Test
    fun reportText() {
        val a = N1()
        val b = N1()

        val ann1 = DocumentationAnnotationInstance(a, "Hello A")

        assertEquals(true, ann1.attached)
        assertEquals(listOf(ann1), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann1), a.getAnnotations(Annotation::class))
        assertEquals(listOf(),  b.getAnnotations(Annotation::class))
        a.removeAnnotation(ann1)
        assertEquals(false, ann1.attached)
        assertEquals(listOf(), a.getAnnotations())
        assertEquals(listOf(), a.getAnnotations(Annotation::class))


        val ann2 = DocumentationAnnotationInstance(a, "Hello A, again")
        assertEquals(true, ann2.attached)
        val ann3 = DocumentationAnnotationInstance(a, "Hello A, third time is a charm")
        assertEquals(false, ann2.attached)
        assertEquals(true, ann3.attached)
        assertEquals(listOf(ann3), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann3), a.getAnnotations(Annotation::class))
        assertEquals(listOf(),  b.getAnnotations(Annotation::class))

    }

}
