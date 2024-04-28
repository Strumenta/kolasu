package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.traversing.TraversingStructurallyTest.Box
import com.strumenta.kolasu.traversing.TraversingStructurallyTest.Item
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

internal class TraversingByRangeTest {
    object StarLasuLanguageInstance : StarLasuLanguage("com.strumenta.kolasu.traversing.TraversingByRangeTest") {
        init {
            explore(
                Box::class,
                Item::class,
            )
        }
    }

    class Box(
        val name: String,
        val contents: List<NodeLike>,
        specifiedRange: Range? = null,
    ) : Node(specifiedRange)

    class Item(
        val name: String,
        specifiedRange: Range? = null,
    ) : Node(specifiedRange)

    init {
        StarLasuLanguageInstance.ensureIsRegistered()
    }

    private fun printSequence(sequence: Sequence<NodeLike>): String =
        sequence
            .map {
                when (it) {
                    is Box -> it.name
                    is Item -> it.name
                    else -> fail("")
                }
            }.joinToString()

    private val testCase =
        Box(
            "root",
            listOf(
                Box(
                    "first",
                    listOf(
                        Item("1", specifiedRange = Range(3, 6, 3, 12)),
                    ),
                    specifiedRange = Range(2, 3, 4, 3),
                ),
                Item("2", specifiedRange = Range(5, 3, 5, 9)),
                Box(
                    "big",
                    listOf(
                        Box(
                            "small",
                            listOf(
                                Item("3", specifiedRange = Range(8, 7, 8, 13)),
                                Item("4", specifiedRange = Range(9, 7, 9, 13)),
                                Item("5", specifiedRange = Range(10, 7, 10, 13)),
                            ),
                            specifiedRange = Range(7, 5, 11, 5),
                        ),
                    ),
                    specifiedRange = Range(6, 3, 12, 3),
                ),
                Item("6", specifiedRange = Range(13, 3, 13, 9)),
            ),
            specifiedRange = Range(1, 1, 14, 1),
        )

    @Test
    fun walkWithinWithOutsideRange() {
        val range: Range = Range(15, 1, 15, 1)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("", result)
    }

    @Test
    fun walkWithinWithRootRange() {
        val range: Range = Range(1, 1, 14, 1)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("root, first, 1, 2, big, small, 3, 4, 5, 6", result)
    }

    @Test
    fun walkWithinWithLeafRange() {
        val range: Range = Range(13, 3, 13, 9)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("6", result)
    }

    @Test
    fun walkWithinPointInLeafRange() {
        val range: Range = Range(13, 4, 13, 5)
        assertEquals("", printSequence(testCase.walkWithin(range)))
    }

    @Test
    fun walkWithinWithSubTreeRange() {
        val range: Range = Range(7, 5, 11, 5)
        val result: String = printSequence(testCase.walkWithin(range))
        assertEquals("small, 3, 4, 5", result)
    }

    @Test
    fun findByRange() {
        val leaf1: Range = Range(13, 4, 13, 5)
        assertEquals("root, 6", printSequence(testCase.searchByRange(leaf1, true)))
        assertEquals("6", printSequence(sequenceOf(testCase.findByRange(leaf1, true)!!)))
        assertEquals("root, 6", printSequence(testCase.searchByRange(leaf1, false)))
        assertEquals("6", printSequence(sequenceOf(testCase.findByRange(leaf1, false)!!)))

        val leaf2: Range = Range(10, 8, 10, 12)
        assertEquals("root, big, small, 5", printSequence(testCase.searchByRange(leaf2, true)))
        assertEquals("5", printSequence(sequenceOf(testCase.findByRange(leaf2, true)!!)))
        assertEquals("root, big, small, 5", printSequence(testCase.searchByRange(leaf2, false)))
        assertEquals("5", printSequence(sequenceOf(testCase.findByRange(leaf2, false)!!)))

        val internal: Range = Range(8, 8, 10, 12)
        assertEquals("root, big, small", printSequence(testCase.searchByRange(internal, true)))
        assertEquals("small", printSequence(sequenceOf(testCase.findByRange(internal, true)!!)))
        assertEquals("root, big, small", printSequence(testCase.searchByRange(internal, false)))
        assertEquals("small", printSequence(sequenceOf(testCase.findByRange(internal, false)!!)))

        val outside: Range = Range(100, 100, 101, 101)
        assertEquals("", printSequence(testCase.searchByRange(outside, true)))
        assertNull(testCase.findByRange(outside, true))
    }
}
