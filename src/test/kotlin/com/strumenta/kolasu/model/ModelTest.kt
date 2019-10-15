package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import org.junit.Test as test

class MyNode(override val name: String) : Node(), Named

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
}
