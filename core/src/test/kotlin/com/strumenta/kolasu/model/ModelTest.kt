package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test as test

class MyNode(override val name: String) : Node(), Named

data class ASymbol(override val name: String, val index: Int = 0) : Symbol
data class BSymbol(override val name: String, val index: Int = 0) : Symbol

class ModelTest {

    @test
    fun referenceByNameUnsolvedToString() {
        val refUnsolved = ReferenceByName<MyNode>("foo")
        assertEquals("Ref(foo)[Unsolved]", refUnsolved.toString())
    }

    @test
    fun referenceByNameSolvedToString() {
        val refSolved = ReferenceByName<MyNode>("foo", MyNode("foo"))
        assertEquals("Ref(foo)[Solved]", refSolved.toString())
    }

    @test
    fun tryToResolvePositiveCaseSameCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(true, ref.tryToResolve(listOf(MyNode("foo"))))
        assertEquals(true, ref.resolved)
    }

    @test
    fun tryToResolveNegativeCaseSameCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(false, ref.tryToResolve(listOf(MyNode("foo2"))))
        assertEquals(false, ref.resolved)
    }

    @test
    fun tryToResolvePositiveCaseDifferentCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(true, ref.tryToResolve(listOf(MyNode("fOo")), caseInsensitive = true))
        assertEquals(true, ref.resolved)
    }

    @test
    fun tryToResolveNegativeCaseDifferentCase() {
        val ref = ReferenceByName<MyNode>("foo")
        assertEquals(false, ref.tryToResolve(listOf(MyNode("foO"))))
        assertEquals(false, ref.resolved)
    }

    @test
    fun addToScope() {
        val scope = Scope()
        assertTrue { scope.symbols.isEmpty() }

        scope.add(ASymbol(name = "a"))
        assertTrue { scope.symbols.size == 1 }
        assertTrue { scope.symbols["a"]!!.size == 1 }
        assertTrue { scope.symbols["a"]!![0] == ASymbol(name = "a") }

        scope.add(BSymbol(name = "a", index = 1))
        assertTrue { scope.symbols.size == 1 }
        assertTrue { scope.symbols["a"]!!.size == 2 }
        assertTrue { scope.symbols["a"]!![0] == ASymbol(name = "a") }
        assertTrue { scope.symbols["a"]!![1] == BSymbol(name = "a", index = 1) }
    }

    @test
    fun lookupSymbolByNameInLocal() {
        val scope = Scope(ASymbol(name = "a"), parent = Scope(ASymbol(name = "b")))
        val expected = ASymbol(name = "a")
        val actual = scope.lookup(symbolName = "a")
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameInParent() {
        val scope = Scope(ASymbol(name = "a"), parent = Scope(ASymbol(name = "b")))
        val expected = ASymbol(name = "b")
        val actual = scope.lookup(symbolName = "b")
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameNotFound() {
        val scope = Scope(ASymbol(name = "b"))
        assertNull(scope.lookup(symbolName = "a"))
    }

    @test
    fun lookupSymbolByNameAndTypeInLocal() {
        val scope = Scope(ASymbol(name = "a"), parent = Scope(BSymbol(name = "a")))
        val expected = ASymbol(name = "a")
        val actual = scope.lookup(symbolName = "a", symbolType = ASymbol::class.java)
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameAndTypeInParent() {
        val scope = Scope(ASymbol(name = "a"), parent = Scope(BSymbol(name = "a")))
        val expected = BSymbol(name = "a")
        val actual = scope.lookup("a", BSymbol::class.java)
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameAndTypeNotFoundDifferentName() {
        val scope = Scope(ASymbol(name = "a"))
        assertNull(scope.lookup(symbolName = "b", symbolType = ASymbol::class.java))
    }

    @test
    fun lookupSymbolByNameAndTypeNotFoundDifferentType() {
        val scope = Scope(ASymbol(name = "a"))
        assertNull(scope.lookup(symbolName = "a", symbolType = BSymbol::class.java))
    }
}
