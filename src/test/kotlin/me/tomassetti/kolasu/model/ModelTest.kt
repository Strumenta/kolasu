package me.tomassetti.kolasu.model

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
}