package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

internal class TraversingStructurallyTest {
    class Box(
        val name: String,
        val contents: List<Node> = listOf(),
        val set: Set<Node> = setOf(),
        specifiedPosition: Position? = null
    ) : Node(specifiedPosition)
    class Item(val name: String, specifiedPosition: Position? = null) : Node(specifiedPosition)

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
                    Item("1", specifiedPosition = pos(3, 6, 3, 12))
                ),
                specifiedPosition = pos(2, 3, 4, 3)
            ),
            Item("2", specifiedPosition = pos(5, 3, 5, 9)),
            Box(
                "big",
                listOf(
                    Box(
                        "small",
                        listOf(
                            Item("3", specifiedPosition = pos(8, 7, 8, 13)),
                            Item("4", specifiedPosition = pos(9, 7, 9, 13)),
                            Item("5", specifiedPosition = pos(10, 7, 10, 13))
                        ),
                        specifiedPosition = pos(7, 5, 11, 5)
                    )
                ),
                specifiedPosition = pos(6, 3, 12, 3)
            ),
            Item("6", specifiedPosition = pos(13, 3, 13, 9))
        ),
        specifiedPosition = pos(1, 1, 14, 1)
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

    @Test
    fun walkSet() {
        // Note: we use hashSetOf() specifically because it doesn't guarantee traversal order
        val testCase = Box(
            "root",
            set = hashSetOf(
                Box(
                    "first",
                    set = hashSetOf(
                        Item("1", specifiedPosition = pos(3, 6, 3, 12))
                    ),
                    specifiedPosition = pos(2, 3, 4, 3)
                ),
                Item("2", specifiedPosition = pos(5, 3, 5, 9)),
                Box(
                    "big",
                    set = hashSetOf(
                        Box(
                            "small",
                            set = hashSetOf(
                                Item("3", specifiedPosition = pos(8, 7, 8, 13)),
                                Item("4", specifiedPosition = pos(9, 7, 9, 13)),
                                Item("5", specifiedPosition = pos(10, 7, 10, 13))
                            ),
                            specifiedPosition = pos(7, 5, 11, 5)
                        )
                    ),
                    specifiedPosition = pos(6, 3, 12, 3)
                ),
                Item("6", specifiedPosition = pos(13, 3, 13, 9))
            ),
            specifiedPosition = pos(1, 1, 14, 1)
        )
        val set = mutableSetOf<String>()
        testCase.walk().map {
            when (it) {
                is Box -> it.name
                is Item -> it.name
                else -> fail("")
            }
        }.forEach {
            set.add(it)
        }
        assertEquals(setOf("root", "first", "1", "2", "big", "small", "3", "4", "5", "6"), set)
    }
}
