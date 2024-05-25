package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

val myLanguage = StarLasuLanguage("foo.bar.MyLanguage")
val mySingleAnnotation = Annotation(myLanguage, "MySingleAnnotation")

class MySingleAnnotation : AnnotationInstance(mySingleAnnotation)

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
        assertEquals(emptyList(), n1.allAnnotationInstances)
        assertEquals(emptyList(), n1.annotationsByType(mySingleAnnotation))
        n1.addAnnotation(a1)
        assertEquals(listOf(a1), n1.allAnnotationInstances)
        assertEquals(true, n1.hasAnnotation(a1))
        assertEquals(true, a1.isAttached)
        assertEquals(n1, a1.annotatedNode)
        assertEquals(listOf(a1), n1.annotationsByType(mySingleAnnotation))
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
        assertEquals(emptyList(), n1.allAnnotationInstances)
        assertEquals(emptyList(), n1.annotationsByType(mySingleAnnotation))
    }
}
