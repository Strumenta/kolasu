package com.strumenta.kolasu.model

import com.strumenta.kolasu.model.annotations.Annotation
import com.strumenta.kolasu.model.annotations.MultipleAnnotation
import com.strumenta.kolasu.model.annotations.SingleAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

data class DocumentationAnnotation(val text: String)
    : SingleAnnotation()

data class CommentAnnotation(val text: String)
    : MultipleAnnotation()

class N1 : Node()

class AnnotationsTest {

    @Test
    fun singleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(DocumentationAnnotation("Hello A"))

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

        val ann2 = a.addAnnotation(DocumentationAnnotation("Hello A, again"))
        assertEquals(true, ann2.attached)
        val ann3 = a.addAnnotation(DocumentationAnnotation("Hello A, third time is a charm"))
        assertEquals(false, ann2.attached)
        assertEquals(null, ann2.annotatedNode)
        assertEquals(true, ann3.attached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann3), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann3), a.getAnnotations(DocumentationAnnotation::class))
        assertEquals(listOf(),  b.getAnnotations(DocumentationAnnotation::class))
    }

    @Test
    fun multipleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(CommentAnnotation("Hello A"))

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

        val ann2 = a.addAnnotation(CommentAnnotation("Hello A, again"))
        assertEquals(true, ann2.attached)
        val ann3 = a.addAnnotation(CommentAnnotation("Hello A, third time is a charm"))
        assertEquals(true, ann2.attached)
        assertEquals(a, ann2.annotatedNode)
        assertEquals(true, ann3.attached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann2, ann3), a.getAnnotations())
        assertEquals(listOf(),  b.getAnnotations())
        assertEquals(listOf(ann2, ann3), a.getAnnotations(CommentAnnotation::class))
        assertEquals(listOf(),  b.getAnnotations(CommentAnnotation::class))

    }

}
