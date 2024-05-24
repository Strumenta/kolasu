package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.Multiplicity
import kotlin.test.DefaultAsserter.fail
import kotlin.test.Test
import kotlin.test.assertEquals

class MyNode : MPNode()

enum class MyEnum

class MetamodelTest {
    @Test
    fun attributeMultiplicity() {
        assertEquals(Multiplicity.OPTIONAL, Property("MyAttribute", true, stringType, { TODO() }).multiplicity)
        assertEquals(Multiplicity.SINGULAR, Property("MyAttribute", false, stringType, { TODO() }).multiplicity)
    }

    @Test
    fun referenceMultiplicity() {
        val l = StarLasuLanguage("my.foo.language")
        assertEquals(
            Multiplicity.OPTIONAL,
            Reference("MyReference", true, Concept(l, "com.strumenta.kolasu.language.MyNode"), { TODO() }).multiplicity,
        )
        assertEquals(
            Multiplicity.SINGULAR,
            Reference(
                "MyReference",
                false,
                Concept(l, "com.strumenta.kolasu.language.MyNode"),
                { TODO() },
            ).multiplicity,
        )
    }
}

fun assertThrows(op: () -> Unit) {
    try {
        op.invoke()
        fail("exception not thrown")
    } catch (t: Throwable) {
    }
}
