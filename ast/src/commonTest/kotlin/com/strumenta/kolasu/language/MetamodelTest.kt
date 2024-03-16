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
        assertEquals(Multiplicity.OPTIONAL, Attribute("MyAttribute", true, stringType).multiplicity)
        assertEquals(Multiplicity.SINGULAR, Attribute("MyAttribute", false, stringType).multiplicity)
    }

    @Test
    fun referenceMultiplicity() {
        assertEquals(
            Multiplicity.OPTIONAL,
            Reference("MyReference", true, Concept("com.strumenta.kolasu.language.MyNode")).multiplicity,
        )
        assertEquals(
            Multiplicity.SINGULAR,
            Reference("MyReference", false, Concept("com.strumenta.kolasu.language.MyNode")).multiplicity,
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
