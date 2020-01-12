package com.strumenta.kolasu.model

import java.lang.UnsupportedOperationException
import java.util.LinkedList
import kotlin.test.assertEquals
import org.junit.Test as test

data class A(val s: String) : Node()
data class B(val a: A, val manyAs: List<A>) : Node()

data class AW(var s: String) : Node()
data class BW(var a: AW, val manyAs: MutableList<AW>) : Node()
data class CW(var a: AW, val manyAs: MutableSet<AW>) : Node()

@NodeType
interface FooNodeType

interface BarNotNodeType

class ProcessingTest {

    @test
    fun recognizeNodeType() {
        assertEquals(true, FooNodeType::class.isMarkedAsNodeType())
        assertEquals(false, BarNotNodeType::class.isMarkedAsNodeType())

        assertEquals(true, FooNodeType::class.representsNode())
        assertEquals(false, BarNotNodeType::class.representsNode())
    }

    @test(expected = ImmutablePropertyException::class)
    fun replaceSingleOnReadOnly() {
        val a1 = A("1")
        val a2 = A("2")
        val b = B(a1, emptyList())
        b.assignParents()
        a1.replace(a2)
    }

    @test fun replaceSingle() {
        val a1 = AW("1")
        val a2 = AW("2")
        val b = BW(a1, LinkedList())
        b.assignParents()
        a1.replace(a2)
        assertEquals("2", b.a.s)
    }

    @test fun replaceInList() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, mutableListOf(a2, a3))
        b.assignParents()
        a2.replace(a4)
        assertEquals("4", b.manyAs[0].s)
    }

    @test(expected = UnsupportedOperationException::class)
    fun replaceInSet() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = CW(a1, mutableSetOf(a2, a3))
        b.assignParents()
        a2.replace(a4)
    }
}
