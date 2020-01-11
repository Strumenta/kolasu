package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class TraversingTest {
    class Box(val name: String, val contents: List<Node>) : Node()
    class Item(val name: String) : Node()

    private fun printSequence(sequence: Sequence<Node>): String {
        return sequence.map {
            when (it) {
                is Box -> it.name
                is Item -> it.name
                else -> fail("")
            }
        }.joinToString()
    }

    private val testCase = Box(
        "root",
        listOf(
            Box(
                "first",
                listOf(
                    Item("1")
                )
            ),
            Item("2"),
            Box(
                "big",
                listOf(
                    Box(
                        "small",
                        listOf(
                            Item("3"), Item("4"), Item("5")
                        )
                    )
                )
            ),
            Item("6")
        )
    )

    @Test
    fun walkDepthFirst() {
        val result: String = printSequence(testCase.walk())
        assertEquals("root, first, 1, 2, big, small, 3, 4, 5, 6", result)
    }

    @Test
    fun walkLeavesFirst() {
        val result: String = printSequence(testCase.walkLeavesFirst())
        assertEquals("1, first, 2, 3, 4, 5, small, big, 6, root", result)
    }

    @Test
    fun walkDescendants() {
        val result: String = printSequence(testCase.walkDescendants())
        assertEquals("first, 1, 2, big, small, 3, 4, 5, 6", result)
    }

    @Test
    fun walkAncestors() {
        testCase.assignParents()
        val item4 = ((testCase.contents[2] as Box).contents[0] as Box).contents[1]
        val result: String = printSequence(item4.walkAncestors())
        assertEquals("small, big, root", result)
    }
}
