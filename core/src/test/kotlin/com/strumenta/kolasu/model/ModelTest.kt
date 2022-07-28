package com.strumenta.kolasu.model

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test as test

class MyNode(override val name: String) : Node(), Named

data class ASymbol(override val name: String, val index: Int = 0) : Symbol
data class BSymbol(override val name: String, val index: Int = 0) : Symbol

data class USymbol(override val name: String? = null) : Symbol

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
    fun scopeAddSymbols() {
        val scope = Scope()
        assertTrue { scope.getSymbols().isEmpty() }

        scope.add(ASymbol(name = "a", index = 0))
        assertEquals(1, scope.getSymbols().size)
        assertEquals(1, scope.getSymbols().getOrElse("a") { emptyList() }.size)
        assertContains(scope.getSymbols().getOrElse("a") { emptyList() }, ASymbol(name = "a", index = 0))

        scope.add(BSymbol(name = "b", index = 0))
        assertEquals(2, scope.getSymbols().size)
        assertEquals(1, scope.getSymbols().getOrElse("b") { emptyList() }.size)
        assertContains(scope.getSymbols().getOrElse("b") { emptyList() }, BSymbol(name = "b", index = 0))

        scope.add(ASymbol(name = "b", index = 1))
        assertEquals(2, scope.getSymbols().size)
        assertEquals(2, scope.getSymbols().getOrElse("b") { emptyList() }.size)
        assertContains(scope.getSymbols().getOrElse("b") { emptyList() }, BSymbol(name = "b", index = 0))
        assertContains(scope.getSymbols().getOrElse("b") { emptyList() }, ASymbol(name = "b", index = 1))
    }

    @test(expected = IllegalArgumentException::class)
    fun scopeAddSymbolWithoutNameError() {
        Scope().add(USymbol(name = null))
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
        val actual = scope.lookup(symbolName = "a", symbolType = ASymbol::class)
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameAndTypeInParent() {
        val scope = Scope(ASymbol(name = "a"), parent = Scope(BSymbol(name = "a")))
        val expected = BSymbol(name = "a")
        val actual = scope.lookup("a", BSymbol::class)
        assertEquals(expected, actual)
    }

    @test
    fun lookupSymbolByNameAndTypeNotFoundDifferentName() {
        val scope = Scope(ASymbol(name = "a"))
        assertNull(scope.lookup(symbolName = "b", symbolType = ASymbol::class))
    }

    @test
    fun lookupSymbolByNameAndTypeNotFoundDifferentType() {
        val scope = Scope(ASymbol(name = "a"))
        assertNull(scope.lookup(symbolName = "a", symbolType = BSymbol::class))
    }
}
