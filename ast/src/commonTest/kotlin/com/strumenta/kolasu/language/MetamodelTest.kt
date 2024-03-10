package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.Multiplicity
import kotlin.test.Test
import kotlin.test.assertEquals

class MyNode : MPNode()

class MetamodelTest {
    @Test
    fun attributeMultiplicity() {
        assertEquals(Multiplicity.OPTIONAL, Attribute("MyAttribute", true, stringType).multiplicity)
        assertEquals(Multiplicity.SINGULAR, Attribute("MyAttribute", false, stringType).multiplicity)
    }

    @Test
    fun referenceMultiplicity() {
        assertEquals(Multiplicity.OPTIONAL, Reference("MyReference", true, MyNode::class).multiplicity)
        assertEquals(Multiplicity.SINGULAR, Reference("MyReference", false, MyNode::class).multiplicity)
    }
}
