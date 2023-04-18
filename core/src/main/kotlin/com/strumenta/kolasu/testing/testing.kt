package com.strumenta.kolasu.testing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ParsingResult
import kotlin.test.assertEquals
import kotlin.test.fail

class IgnoreChildren<N : Node> : List<N> {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun contains(element: N): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsAll(elements: Collection<N>): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(index: Int): N {
        TODO("Not yet implemented")
    }

    override fun indexOf(element: N): Int {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun iterator(): Iterator<N> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: N): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): ListIterator<N> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): ListIterator<N> {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<N> {
        TODO("Not yet implemented")
    }
}

class ASTDifferenceException(val context: String, val expected: Any, val actual: Any) :
    Exception("$context: expecting $expected, actual $actual")

fun <T : Node> assertParsingResultsAreEqual(expected: ParsingResult<T>, actual: ParsingResult<T>) {
    assertEquals(expected.issues, actual.issues)
    assertEquals(expected.root != null, actual.root != null)
    if (expected.root != null) {
        assertASTsAreEqual(expected.root, actual.root!!)
    }
}

fun <N : Node> assertASTsAreEqual(
    expected: Node,
    actual: ParsingResult<N>,
    context: String = "<root>",
    considerPosition: Boolean = false
) {
    assertEquals(0, actual.issues.size, actual.issues.toString())
    assertASTsAreEqual(
        expected = expected, actual = actual.root!!, context = context,
        considerPosition = considerPosition
    )
}

fun assertASTsAreEqual(
    expected: Node,
    actual: Node,
    context: String = "<root>",
    considerPosition: Boolean = false
) {
    if (expected::class == actual::class) {
        if (considerPosition) {
            assertEquals(expected.position, actual.position, "$context.position")
        }
        expected.properties.forEach { expectedProperty ->
            val actualPropValue = actual.properties.find { it.name == expectedProperty.name }!!.value
            val expectedPropValue = expectedProperty.value
            if (expectedProperty.provideNodes) {
                if (expectedProperty.multiple) {
                    if (expectedPropValue is IgnoreChildren<*>) {
                        // Nothing to do
                    } else {
                        val actualPropValueCollection = actualPropValue?.let { it as Collection<Node> }
                        val expectedPropValueCollection = expectedPropValue?.let { it as Collection<Node> }
                        assertEquals(
                            actualPropValueCollection == null, expectedPropValueCollection == null,
                            "$context.${expectedProperty.name} nullness"
                        )
                        if (actualPropValueCollection != null && expectedPropValueCollection != null) {
                            assertEquals(
                                expectedPropValueCollection?.size, actualPropValueCollection?.size,
                                "$context.${expectedProperty.name} length"
                            )
                            val expectedIt = expectedPropValueCollection.iterator()
                            val actualIt = actualPropValueCollection.iterator()
                            for (i in expectedPropValueCollection.indices) {
                                assertASTsAreEqual(expectedIt.next(), actualIt.next(), "$context[$i]")
                            }
                        }
                    }
                } else {
                    if (expectedPropValue == null && actualPropValue != null) {
                        assertEquals<Any?>(
                            expectedPropValue, actualPropValue,
                            "$context.${expectedProperty.name}"
                        )
                    } else if (expectedPropValue != null && actualPropValue == null) {
                        assertEquals<Any?>(
                            expectedPropValue, actualPropValue,
                            "$context.${expectedProperty.name}"
                        )
                    } else if (expectedPropValue == null && actualPropValue == null) {
                        // that is ok
                    } else {
                        assertASTsAreEqual(
                            expectedPropValue as Node, actualPropValue as Node,
                            context = "$context.${expectedProperty.name}"
                        )
                    }
                }
            } else {
                assertEquals(
                    expectedPropValue, actualPropValue,
                    "$context, comparing property ${expectedProperty.name}"
                )
            }
        }
    } else {
        fail(
            "$context: expected node of type ${expected::class.qualifiedName}, " +
                "but found ${actual::class.qualifiedName}"
        )
    }
}
