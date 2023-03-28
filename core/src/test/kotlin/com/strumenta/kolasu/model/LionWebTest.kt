package com.strumenta.kolasu.model.tests

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.model.lionweb.ReflectionBasedMetamodel
import com.strumenta.kolasu.model.lionweb.concept
import org.lionweb.lioncore.java.metamodel.LionCoreBuiltins
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

object Metamodel : ReflectionBasedMetamodel("MyMetamodelID", "MyMetamodelFoo", MySimpleNode::class)

class MySimpleNode(val b: Boolean, val others: List<MyOtherNode>) : ASTNode()

class MyOtherNode(val i: Int, val s: String) : ASTNode()

class LionWebTest {

    @Test
    @Ignore
    fun metamodelElements() {
        val mm = MySimpleNode::class.concept.metamodel!!
        assertEquals(2, mm.elements.size, mm.elements.joinToString(", ") { it.qualifiedName() })
    }

    @Test
    fun getConceptMySimpleNodeStatically() {
        //val mm = Metamodel
        val c = MySimpleNode::class.concept
        assertEquals("MySimpleNode", c.simpleName)
        assertEquals("MyMetamodelFoo", c.metamodel?.name)
        assertEquals("MyMetamodelID", c.metamodel?.id)

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
    fun getConceptMyOtherNodeStatically() {
        val c = MyOtherNode::class.concept
        assertEquals("MyOtherNode", c.simpleName)
        assertEquals("MyMetamodelFoo.MyOtherNode", c.qualifiedName())
        assertEquals("MyMetamodelFoo", c.metamodel?.name)
        assertEquals("MyMetamodelID", c.metamodel?.id)

        assertEquals(2, c.allProperties().size)

        val iProperty = c.getPropertyByName("i")!!
        assertEquals("i", iProperty.simpleName)
        assertEquals(LionCoreBuiltins.getInteger(), iProperty.type)
        assertEquals(false, iProperty.isDerived)
        assertEquals(false, iProperty.isOptional)
        assertEquals(true, iProperty.isRequired)

        val sProperty = c.getPropertyByName("s")!!
        assertEquals("s", sProperty.simpleName)
        assertEquals(LionCoreBuiltins.getString(), sProperty.type)
        assertEquals(false, sProperty.isDerived)
        assertEquals(false, sProperty.isOptional)
        assertEquals(true, sProperty.isRequired)

        assertEquals(0, c.allContainments().size)

        assertEquals(0, c.allReferences().size)
    }

    @Test
    fun getConceptMySimpleNode() {
        val node = MySimpleNode(false, emptyList())
        val c = node.concept

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
    fun getPropertyValue() {
        val concept1 = MySimpleNode::class.concept
        val concept2 = MyOtherNode::class.concept

        val node1 = MySimpleNode(false, emptyList())
        assertEquals(false, node1.getPropertyValue(concept1.getPropertyByName("b")!!))

        val node2 = MySimpleNode(true, emptyList())
        assertEquals(true, node2.getPropertyValue(concept1.getPropertyByName("b")!!))

        val node3 = MyOtherNode(79, "Foo")
        assertEquals(79, node3.getPropertyValue(concept2.getPropertyByName("i")!!))
        assertEquals("Foo", node3.getPropertyValue(concept2.getPropertyByName("s")!!))

        val node4 = MyOtherNode(-12, "Bar")
        assertEquals(-12, node4.getPropertyValue(concept2.getPropertyByName("i")!!))
        assertEquals("Bar", node4.getPropertyValue(concept2.getPropertyByName("s")!!))
    }
}
