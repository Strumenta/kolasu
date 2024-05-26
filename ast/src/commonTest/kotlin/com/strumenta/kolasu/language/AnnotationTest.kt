package com.strumenta.kolasu.language

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

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

    @Test
    fun annotationCannotAnnotateAnotherAnnotation() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        val a2 = Annotation(l, "Ann2")
        assertFailsWith<IllegalArgumentException> { a.annotates = a2 }
    }

    @Test
    fun annotationCannotAnnotateItself() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        assertFailsWith<IllegalArgumentException> { a.annotates = a }
    }

    @Test
    fun annotationCanAnnotateConcept() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        val c = Concept(l, "Concept")
        a.annotates = c
        assertSame(c, a.annotates)
    }

    @Test
    fun annotationCanAnnotateConceptInterface() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        val ci = ConceptInterface(l, "ConceptInterface")
        a.annotates = ci
        assertSame(ci, a.annotates)
    }
}
