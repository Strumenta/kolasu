package com.strumenta.kolasu.model

import com.strumenta.kolasu.symbolresolution.Scope
import com.strumenta.kolasu.symbolresolution.Symbol
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test as test

object DocumentationAnnotation : SingleAnnotationType<DocumentationAnnotationInstance>("Documentation")

class DocumentationAnnotationInstance(annotatedNode: Node, val text: String)
    : AnnotationInstance(DocumentationAnnotation, annotatedNode)

class N1 : Node()

class AnnotationsTest {

    @Test
    fun reportText() {
        val a = N1()
        val b = N1()
        val c = N1()
        val ann1 = DocumentationAnnotationInstance(a, "Hello A")
        assertEquals(listOf(ann1), a.getAnnotations())
        assertEquals(listOf(ann1), a.getAnnotations(AnnotationInstance::class))
        a.removeAnnotation(ann1)
        assertEquals(listOf(), a.getAnnotations())
        assertEquals(listOf(), a.getAnnotations(AnnotationInstance::class))
    }

}
