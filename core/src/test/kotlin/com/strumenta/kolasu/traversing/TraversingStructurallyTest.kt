package com.strumenta.kolasu.traversing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.assignParents
import com.strumenta.kolasu.model.range
import kotlin.system.measureTimeMillis
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.fail

internal class TraversingStructurallyTest {
    class Box(
        val name: String,
        val contents: List<NodeLike> = listOf(),
        val set: Set<NodeLike> = setOf(),
        specifiedRange: Range? = null,
    ) : Node(specifiedRange)

    class Item(
        val name: String,
        specifiedRange: Range? = null,
    ) : Node(specifiedRange)

    private fun printSequence(sequence: Sequence<NodeLike>): String {
        return sequence
            .map {
                when (it) {
                    is Box -> it.name
                    is Item -> it.name
                    else -> fail("")
                }
            }.joinToString()
    }

    private val testCase =
        Box(
            "root",
            listOf(
                Box(
                    "first",
                    listOf(
                        Item("1", specifiedRange = range(3, 6, 3, 12)),
                    ),
                    specifiedRange = range(2, 3, 4, 3),
                ),
                Item("2", specifiedRange = range(5, 3, 5, 9)),
                Box(
                    "big",
                    listOf(
                        Box(
                            "small",
                            listOf(
                                Item("3", specifiedRange = range(8, 7, 8, 13)),
                                Item("4", specifiedRange = range(9, 7, 9, 13)),
                                Item("5", specifiedRange = range(10, 7, 10, 13)),
                            ),
                            specifiedRange = range(7, 5, 11, 5),
                        ),
                    ),
                    specifiedRange = range(6, 3, 12, 3),
                ),
                Item("6", specifiedRange = range(13, 3, 13, 9)),
            ),
            specifiedRange = range(1, 1, 14, 1),
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

    fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    @Test
    @Ignore
    fun performanceTest() {
        val boxes = mutableListOf<Box>()
        val numberOfChildren = 10000
        var nodes: Int = 1
        val numberOfGrandChildren = 10
        for (i in 0..numberOfChildren) {
            val nChildren = (0..numberOfGrandChildren).random()
            val children = mutableListOf<NodeLike>()
            for (b in 0..nChildren) {
                val grandChildren = mutableListOf<NodeLike>()
                for (c in 0..(0..numberOfGrandChildren).random()) {
                    grandChildren.add(Item(getRandomString(8)))
                    nodes += 1
                }
                children.add(Box(getRandomString(8), grandChildren))
                nodes += 1
            }
            nodes += 1
            boxes.add(Box(getRandomString(8), children))
        }
        val root = Box("root", boxes)
        root.assignParents()

        val countedNodesList: MutableList<Int> = mutableListOf()
        val walkTime =
            measureTimeMillis {
                var countedNodes = 0
                root.walk().forEach { countedNodes++ }
                countedNodesList.add(countedNodes)
            }
        val walkTimeTwo =
            measureTimeMillis {
                var countedNodes = 0
                root.walk().forEach { countedNodes++ }
                countedNodesList.add(countedNodes)
            }
        val fw = FastWalker(root)
        val walkTimeFast =
            measureTimeMillis {
                var countedNodes = 0
                fw.walk().forEach { countedNodes++ }
                countedNodesList.add(countedNodes)
            }
        val walkTimeFastTwo =
            measureTimeMillis {
                var countedNodes = 0
                fw.walk().forEach { countedNodes++ }
                countedNodesList.add(countedNodes)
            }
        val walkTimeFastThree =
            measureTimeMillis {
                var countedNodes = 0
                fw.walk().forEach { countedNodes++ }
                countedNodesList.add(countedNodes)
            }
        countedNodesList.forEach {
            assertEquals(nodes, it)
        }
        // we are testing that walkTimeFast is taking more or less the same time as walkTime
        assertContains(walkTime * 0.8..walkTime * 1.35, walkTimeFast.toDouble())
        assert(walkTimeFastTwo < walkTime)
        assert(walkTimeFastThree < walkTime)
    }

    @Test
    fun walkSet() {
        // Note: we use hashSetOf() specifically because it doesn't guarantee traversal order
        val testCase =
            Box(
                "root",
                set =
                    hashSetOf(
                        Box(
                            "first",
                            set =
                                hashSetOf(
                                    Item("1", specifiedRange = range(3, 6, 3, 12)),
                                ),
                            specifiedRange = range(2, 3, 4, 3),
                        ),
                        Item("2", specifiedRange = range(5, 3, 5, 9)),
                        Box(
                            "big",
                            set =
                                hashSetOf(
                                    Box(
                                        "small",
                                        set =
                                            hashSetOf(
                                                Item("3", specifiedRange = range(8, 7, 8, 13)),
                                                Item("4", specifiedRange = range(9, 7, 9, 13)),
                                                Item("5", specifiedRange = range(10, 7, 10, 13)),
                                            ),
                                        specifiedRange = range(7, 5, 11, 5),
                                    ),
                                ),
                            specifiedRange = range(6, 3, 12, 3),
                        ),
                        Item("6", specifiedRange = range(13, 3, 13, 9)),
                    ),
                specifiedRange = range(1, 1, 14, 1),
            )
        val set = mutableSetOf<String>()
        testCase
            .walk()
            .map {
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
