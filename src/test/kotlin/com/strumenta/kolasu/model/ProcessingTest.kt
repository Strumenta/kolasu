package com.strumenta.kolasu.model

import java.util.LinkedList
import kotlin.test.assertEquals
import org.junit.Test as test

data class A(val s: String) : Node()
data class B(val a: A, val manyAs: List<A>) : Node()

data class AW(var s: String) : Node()
data class BW(var a: AW, val manyAs: MutableList<AW>) : Node()

class ProcessingTest {

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

    @test fun replaceList() {
        val a1 = AW("1")
        val a2 = AW("2")
        val a3 = AW("3")
        val a4 = AW("4")
        val b = BW(a1, listOf(a2, a3).toMutableList())
        b.assignParents()
        a2.replace(a4)
        assertEquals("4", b.manyAs[0].s)
    }
}
