package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.AnnotationInstance
import com.strumenta.kolasu.model.dynamic.DynamicAnnotationInstance
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
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
    fun instantiateEmptyDynamicInstance() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        val instance = a.instantiate(emptyMap())
        assertIs<DynamicAnnotationInstance>(instance)
        assertSame(a, instance.annotation)
    }

    @Test
    @Ignore
    fun instantiateEmptyClassBasedInstance() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")

        class Ann : AnnotationInstance() {
            override val annotation: Annotation
                get() = a
        }

        a.correspondingKotlinClass = Ann::class

        val instance = a.instantiate(emptyMap())
        assertIs<Ann>(instance)
        assertSame(a, instance.annotation)
    }

}
