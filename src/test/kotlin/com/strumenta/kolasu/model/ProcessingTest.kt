package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.Test as test

data class A(val s: String) : Node()
data class B(val a: A, val manyAs : List<A>) : Node()

class ProcessingTest {

    @test fun replaceSingle() {
        val a1 = A("1")
        val a2 = A("2")
        val b = B(a1, emptyList())
        a1.replace(a2)
        assertEquals("2", b.a.s)
    }

    @test fun replaceList() {
        val a1 = A("1")
        val a2 = A("2")
        val a3 = A("3")
        val a4 = A("4")
        val b = B(a1, listOf(a2, a3))
        a2.replace(a4)
        assertEquals("4", b.manyAs[0].s)
    }

}