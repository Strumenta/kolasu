package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.Multiplicity
import kotlin.test.Test
import kotlin.test.assertEquals

class MyNode : MPNode()

class MetamodelTest {
    @Test
    fun referenceMultiplicity() {
        assertEquals(Multiplicity.OPTIONAL, Reference("MyAttribute", true, MyNode::class).multiplicity)
        assertEquals(Multiplicity.SINGULAR, Reference("MyAttribute", false, MyNode::class).multiplicity)
    }
}
