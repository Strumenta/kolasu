package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals

data class DocumentationAnnotation(
    val text: String,
) : JVMSingleAnnotation()

data class CommentAnnotation(
    val text: String,
) : JVMMultipleAnnotation()

class N1 : Node()

class AnnotationsTest {
    @Test
    fun singleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(DocumentationAnnotation("Hello A"))

        assertEquals(true, ann1.isAttached)
        assertEquals(a, ann1.annotatedNode)
        assertEquals(listOf(ann1), a.allAnnotationInstances)
        assertEquals(listOf(), b.allAnnotationInstances)
        assertEquals(listOf(ann1), a.annotationsByType(AnnotationInstance::class))
        assertEquals(listOf(), b.annotationsByType(AnnotationInstance::class))
        a.removeAnnotation(ann1)
        assertEquals(null, ann1.annotatedNode)
        assertEquals(false, ann1.isAttached)
        assertEquals(listOf(), a.allAnnotationInstances)
        assertEquals(listOf(), a.annotationsByType(AnnotationInstance::class))

        val ann2 = a.addAnnotation(DocumentationAnnotation("Hello A, again"))
        assertEquals(true, ann2.isAttached)
        val ann3 = a.addAnnotation(DocumentationAnnotation("Hello A, third time is a charm"))
        assertEquals(false, ann2.isAttached)
        assertEquals(null, ann2.annotatedNode)
        assertEquals(true, ann3.isAttached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann3), a.allAnnotationInstances)
        assertEquals(listOf(), b.allAnnotationInstances)
        assertEquals(listOf(ann3), a.annotationsByType(DocumentationAnnotation::class))
        assertEquals(listOf(), b.annotationsByType(DocumentationAnnotation::class))
    }

    @Test
    fun multipleAnnotation() {
        val a = N1()
        val b = N1()

        val ann1 = a.addAnnotation(CommentAnnotation("Hello A"))

        assertEquals(true, ann1.isAttached)
        assertEquals(a, ann1.annotatedNode)
        assertEquals(listOf(ann1), a.allAnnotationInstances)
        assertEquals(listOf(), b.allAnnotationInstances)
        assertEquals(listOf(ann1), a.annotationsByType(AnnotationInstance::class))
        assertEquals(listOf(), b.annotationsByType(AnnotationInstance::class))
        a.removeAnnotation(ann1)
        assertEquals(null, ann1.annotatedNode)
        assertEquals(false, ann1.isAttached)
        assertEquals(listOf(), a.allAnnotationInstances)
        assertEquals(listOf(), a.annotationsByType(AnnotationInstance::class))

        val ann2 = a.addAnnotation(CommentAnnotation("Hello A, again"))
        assertEquals(true, ann2.isAttached)
        val ann3 = a.addAnnotation(CommentAnnotation("Hello A, third time is a charm"))
        assertEquals(true, ann2.isAttached)
        assertEquals(a, ann2.annotatedNode)
        assertEquals(true, ann3.isAttached)
        assertEquals(a, ann3.annotatedNode)
        assertEquals(listOf(ann2, ann3), a.allAnnotationInstances)
        assertEquals(listOf(), b.allAnnotationInstances)
        assertEquals(listOf(ann2, ann3), a.annotationsByType(CommentAnnotation::class))
        assertEquals(listOf(), b.annotationsByType(CommentAnnotation::class))
    }
}
