package com.strumenta.kolasu.model

import com.strumenta.kolasu.ast.Annotation
import com.strumenta.kolasu.ast.MultipleAnnotation
import com.strumenta.kolasu.ast.SingleAnnotation
import kotlin.test.Test
import kotlin.test.assertEquals

data class DocumentationAnnotation(
    val text: String,
) : SingleAnnotation()

data class CommentAnnotation(
    val text: String,
) : MultipleAnnotation()

class N1 : Node()

class AnnotationsTest {
    @Test
    fun singleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(DocumentationAnnotation("Hello A"))

        assertEquals(true, ann1.attached)
        assertEquals(a, ann1.annotatedNode)
        assertEquals(listOf(ann1), a.allAnnotations)
        assertEquals(listOf(), b.allAnnotations)
        assertEquals(listOf(ann1), a.annotationsByType(Annotation::class))
        assertEquals(listOf(), b.annotationsByType(Annotation::class))
        a.removeAnnotation(ann1)
        assertEquals(null, ann1.annotatedNode)
        assertEquals(false, ann1.attached)
        assertEquals(listOf(), a.allAnnotations)
        assertEquals(listOf(), a.annotationsByType(Annotation::class))

        val ann2 = a.addAnnotation(DocumentationAnnotation("Hello A, again"))
        assertEquals(true, ann2.attached)
        val ann3 = a.addAnnotation(DocumentationAnnotation("Hello A, third time is a charm"))
        assertEquals(false, ann2.attached)
        assertEquals(null, ann2.annotatedNode)
        assertEquals(true, ann3.attached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann3), a.allAnnotations)
        assertEquals(listOf(), b.allAnnotations)
        assertEquals(listOf(ann3), a.annotationsByType(DocumentationAnnotation::class))
        assertEquals(listOf(), b.annotationsByType(DocumentationAnnotation::class))
    }

    @Test
    fun multipleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(CommentAnnotation("Hello A"))

        assertEquals(true, ann1.attached)
        assertEquals(a, ann1.annotatedNode)
        assertEquals(listOf(ann1), a.allAnnotations)
        assertEquals(listOf(), b.allAnnotations)
        assertEquals(listOf(ann1), a.annotationsByType(Annotation::class))
        assertEquals(listOf(), b.annotationsByType(Annotation::class))
        a.removeAnnotation(ann1)
        assertEquals(null, ann1.annotatedNode)
        assertEquals(false, ann1.attached)
        assertEquals(listOf(), a.allAnnotations)
        assertEquals(listOf(), a.annotationsByType(Annotation::class))

        val ann2 = a.addAnnotation(CommentAnnotation("Hello A, again"))
        assertEquals(true, ann2.attached)
        val ann3 = a.addAnnotation(CommentAnnotation("Hello A, third time is a charm"))
        assertEquals(true, ann2.attached)
        assertEquals(a, ann2.annotatedNode)
        assertEquals(true, ann3.attached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann2, ann3), a.allAnnotations)
        assertEquals(listOf(), b.allAnnotations)
        assertEquals(listOf(ann2, ann3), a.annotationsByType(CommentAnnotation::class))
        assertEquals(listOf(), b.annotationsByType(CommentAnnotation::class))
    }
}
