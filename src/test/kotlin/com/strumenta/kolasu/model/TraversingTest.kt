package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class TraversingTest {
    class A(val a: List<Node>) : Node()
    class B(val value: String) : Node()

    @Test
    fun depthFirst() {
        val testCase = A(
            listOf(
                A(listOf(B("XYZ"))),
                B("P"),
                A(listOf(A(listOf(B("1"), B("2"), B("3"))))),
                B("Q")
            )
        )
        val result: String = testCase.descendants().map {
            when (it) {
                is A -> "[A]"
                is B -> "[B ${it.value}]"
                else -> fail("")
            }
        }.joinToString()
        assertEquals("[A], [B XYZ], [B P], [A], [A], [B 1], [B 2], [B 3], [B Q]", result)
    }
}
