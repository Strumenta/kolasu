package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.pos
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

internal class TraversingByRangeTest {
    class Box(
        val name: String,
        val contents: List<Node>,
        specifiedRange: Range? = null
    ) : Node(specifiedRange)
    class Item(val name: String, specifiedRange: Range? = null) : Node(specifiedRange)

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
                    Item("1", specifiedRange = pos(3, 6, 3, 12))
                ),
                specifiedRange = pos(2, 3, 4, 3)
            ),
            Item("2", specifiedRange = pos(5, 3, 5, 9)),
            Box(
                "big",
                listOf(
                    Box(
                        "small",
                        listOf(
                            Item("3", specifiedRange = pos(8, 7, 8, 13)),
                            Item("4", specifiedRange = pos(9, 7, 9, 13)),
                            Item("5", specifiedRange = pos(10, 7, 10, 13))
                        ),
                        specifiedRange = pos(7, 5, 11, 5)
                    )
                ),
                specifiedRange = pos(6, 3, 12, 3)
            ),
            Item("6", specifiedRange = pos(13, 3, 13, 9))
        ),
        specifiedRange = pos(1, 1, 14, 1)
    )

    @Test
    fun walkWithinWithOutsidePosition() {
        val range: Range = pos(15, 1, 15, 1)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("", result)
    }

    @Test
    fun walkWithinWithRootPosition() {
        val range: Range = pos(1, 1, 14, 1)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("root, first, 1, 2, big, small, 3, 4, 5, 6", result)
    }

    @Test
    fun walkWithinWithLeafPosition() {
        val range: Range = pos(13, 3, 13, 9)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("6", result)
    }

    @Test
    fun walkWithinPointInLeafPosition() {
        val range: Range = pos(13, 4, 13, 5)
        assertEquals("", printSequence(testCase.walkWithin(range)))
    }

    @Test
    fun walkWithinWithSubTreePosition() {
        val range: Range = pos(7, 5, 11, 5)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("small, 3, 4, 5", result)
    }

    @Test
    fun findByPosition() {
        val leaf1: Range = pos(13, 4, 13, 5)
        assertEquals("root, 6", printSequence(testCase.searchByPosition(leaf1, true)))
        assertEquals("6", printSequence(sequenceOf(testCase.findByPosition(leaf1, true)!!)))
        assertEquals("root, 6", printSequence(testCase.searchByPosition(leaf1, false)))
        assertEquals("6", printSequence(sequenceOf(testCase.findByPosition(leaf1, false)!!)))

        val leaf2: Range = pos(10, 8, 10, 12)
        assertEquals("root, big, small, 5", printSequence(testCase.searchByPosition(leaf2, true)))
        assertEquals("5", printSequence(sequenceOf(testCase.findByPosition(leaf2, true)!!)))
        assertEquals("root, big, small, 5", printSequence(testCase.searchByPosition(leaf2, false)))
        assertEquals("5", printSequence(sequenceOf(testCase.findByPosition(leaf2, false)!!)))

        val internal: Range = pos(8, 8, 10, 12)
        assertEquals("root, big, small", printSequence(testCase.searchByPosition(internal, true)))
        assertEquals("small", printSequence(sequenceOf(testCase.findByPosition(internal, true)!!)))
        assertEquals("root, big, small", printSequence(testCase.searchByPosition(internal, false)))
        assertEquals("small", printSequence(sequenceOf(testCase.findByPosition(internal, false)!!)))

        val outside: Range = pos(100, 100, 101, 101)
        assertEquals("", printSequence(testCase.searchByPosition(outside, true)))
        assertNull(testCase.findByPosition(outside, true))
    }
}
