package com.strumenta.kolasu.model.reflection

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.AnnotationInstance
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

val l = StarLasuLanguage("my.language")
val a = Annotation(l, "Ann")

class Ann : AnnotationInstance() {
    override val annotation: Annotation
        get() = a
}

class ReflectionBasedInstantiatorTest {
    @Test
    fun instantiateEmptyClassBasedInstance() {
        a.correspondingKotlinClass = Ann::class

        val rbi = ReflectionBasedInstantiator()
        val instance = rbi.instantiate(a, emptyMap())
        assertIs<Ann>(instance)
        assertSame(a, instance.annotation)
    }
}
