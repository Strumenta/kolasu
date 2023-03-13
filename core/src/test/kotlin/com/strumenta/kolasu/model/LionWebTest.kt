package com.strumenta.kolasu.model

import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import kotlin.test.Test
import kotlin.test.assertEquals

class MySimpleNode(val b: Boolean, val others: List<MyOtherNode>) : ASTNode()

class MyOtherNode(val i: Int, val s: String): ASTNode()

class LionWebTest {

    @Test
    fun getConceptMySimpleNodeStatically() {
        val c = MySimpleNode::class.concept
        assertEquals("MySimpleNode", c.simpleName)

        assertEquals(1, c.allProperties().size)

        val bProperty = c.getPropertyByName("b")!!
        assertEquals("b", bProperty.simpleName)
        assertEquals(LionCoreBuiltins.getBoolean(), bProperty.type)
        assertEquals(false, bProperty.isDerived)
        assertEquals(false, bProperty.isOptional)
        assertEquals(true, bProperty.isRequired)

        assertEquals(1, c.allContainments().size)
        val othersContainment = c.getContainmentByName("others")!!
        assertEquals("others", othersContainment.simpleName)
        assertEquals("MyOtherNode", othersContainment.type!!.simpleName)
        assertEquals(false, othersContainment.isDerived)
        assertEquals(false, othersContainment.isOptional)
        assertEquals(true, othersContainment.isMultiple)
        assertEquals(true, othersContainment.isRequired)

        assertEquals(0, c.allReferences().size)
    }

    @Test
    fun getConceptMySimpleNode() {
        val c = MySimpleNode(false, emptyList()).concept
        assertEquals(1, c.allProperties().size)
        assertEquals(1, c.allContainments().size)
        assertEquals(0, c.allReferences().size)
    }
}