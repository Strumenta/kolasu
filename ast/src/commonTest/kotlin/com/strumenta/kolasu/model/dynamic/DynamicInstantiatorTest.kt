package com.strumenta.kolasu.model.dynamic

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertSame

class DynamicInstantiatorTest {
    @Test
    fun instantiateEmptyDynamicInstance() {
        val l = StarLasuLanguage("my.language")
        val a = Annotation(l, "Ann")
        val di = DynamicInstantiator()
        val instance = di.instantiate(a, emptyMap())
        assertIs<DynamicAnnotationInstance>(instance)
        assertSame(a, instance.annotation)
    }
}
