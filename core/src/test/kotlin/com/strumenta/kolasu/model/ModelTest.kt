package com.strumenta.kolasu.model

import com.strumenta.kolasu.serialization.JsonGenerator
import com.strumenta.kolasu.serialization.JsonSerializer
import kotlin.test.assertEquals
import org.junit.Test as test

class MyNode(override val name: String) : Node(), Named

data class NodeOverridingName(
    override var name: String
) : Node(), Named

open class BaseNode(open var attr1: Int) : Node()
data class ExtNode(override var attr1: Int) : BaseNode(attr1)

class ModelTest {

    @test fun referenceByNameUnsolvedToString() {
        val refUnsolved = ReferenceByName<MyNode>("foo")
        assertEquals("Ref(foo)[Unsolved]", refUnsolved.toString())
    }

    @test fun referenceByNameSolvedToString() {
        val refSolved = ReferenceByName<MyNode>("foo", MyNode("foo"))
        assertEquals("Ref(foo)[Solved]", refSolved.toString())
    }

    @test fun tryToResolvePositiveCaseSameCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(true, ref.tryToResolve(listOf(MyNode("foo"))))
        assertEquals(true, ref.resolved)
    }

    @test fun tryToResolveNegativeCaseSameCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(false, ref.tryToResolve(listOf(MyNode("foo2"))))
        assertEquals(false, ref.resolved)
    }

    @test fun tryToResolvePositiveCaseDifferentCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(true, ref.tryToResolve(listOf(MyNode("fOo")), caseInsensitive = true))
        assertEquals(true, ref.resolved)
    }

    @test fun tryToResolveNegativeCaseDifferentCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(false, ref.tryToResolve(listOf(MyNode("foO"))))
        assertEquals(false, ref.resolved)
    }

    @test
    fun duplicatePropertiesInheritedByInterface() {
        val properties = NodeOverridingName::class.nodeProperties
        assertEquals(1, properties.size)
        val json = JsonGenerator().generateString(NodeOverridingName("foo"))
        assertEquals("""{
  "#type": "com.strumenta.kolasu.model.NodeOverridingName",
  "name": "foo"
}""", json)
    }


    @test
    fun duplicatePropertiesInheritedByClass() {
        val properties = ExtNode::class.nodeProperties
        assertEquals(1, properties.size)
        val json = JsonGenerator().generateString(ExtNode(123))
        assertEquals("""{
  "#type": "com.strumenta.kolasu.model.ExtNode",
  "attr1": 123
}""", json)
    }

}
