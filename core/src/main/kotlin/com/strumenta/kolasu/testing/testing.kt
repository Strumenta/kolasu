package com.strumenta.kolasu.testing

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.parsing.toParseTree
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Vocabulary
import kotlin.test.assertEquals
import kotlin.test.fail

fun assertParseTreeStr(
    expectedMultiLineStr: String,
    root: ParserRuleContext,
    vocabulary: Vocabulary,
    printParseTree: Boolean = true
) {
    val actualParseTree = toParseTree(root, vocabulary).multiLineString()
    if (printParseTree) {
        println("parse tree:\n\n${actualParseTree}\n")
    }
    assertEquals(expectedMultiLineStr.trim(), actualParseTree.trim())
}

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
        assertASTsAreEqual(expected.root!!, actual.root!!)
    }
}

fun assertASTsAreEqual(
    expected: Node,
    actual: Node,
    context: String = "<root>"
) {
    if (expected::class == actual::class) {
        expected.properties.forEach { expectedProperty ->
            if (expectedProperty.name == "parseTreeNode") {
                // nothing to do here
            } else {
                val actualPropValue = actual.properties.find { it.name == expectedProperty.name }!!.value
                val expectedPropValue = expectedProperty.value
                if (expectedProperty.provideNodes) {
                    if (expectedProperty.multiple) {
                        if (expectedPropValue is IgnoreChildren<*>) {
                            // Nothing to do
                        } else {
                            val actualPropValueCollection = actualPropValue as Collection<Node>
                            val expectedPropValueCollection = expectedPropValue as Collection<Node>
                            assertEquals(
                                expectedPropValueCollection.size, actualPropValueCollection.size,
                                "$context.${expectedProperty.name} length"
                            )
                            val expectedIt = expectedPropValueCollection.iterator()
                            val actualIt = actualPropValueCollection.iterator()
                            for (i in expectedPropValueCollection.indices) {
                                assertASTsAreEqual(expectedIt.next(), actualIt.next(), "$context[$i]")
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
        }
    } else {
        fail(
            "$context: expected node of type ${expected::class.qualifiedName}, " +
                "but found ${actual::class.qualifiedName}"
        )
    }
}
