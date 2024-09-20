package com.strumenta.kolasu.testing

import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.parsing.toParseTreeModel
import com.strumenta.kolasu.traversing.walkChildren
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Vocabulary
import kotlin.reflect.KClass
import kotlin.test.*

fun assertParseTreeStr(
    expectedMultiLineStr: String,
    root: ParserRuleContext,
    vocabulary: Vocabulary,
    printParseTree: Boolean = true
) {
    val actualParseTree = toParseTreeModel(root, vocabulary).multiLineString()
    if (printParseTree) {
        println("parse tree:\n\n${actualParseTree}\n")
    }
    assertEquals(expectedMultiLineStr.trim(), actualParseTree.trim())
}

class IgnoreChildren<N : Node> : MutableList<N> {
    override val size: Int
        get() = TODO("Not yet implemented")

    override fun clear() {
        TODO("Not yet implemented")
    }

    override fun addAll(elements: Collection<N>): Boolean {
        TODO("Not yet implemented")
    }

    override fun addAll(index: Int, elements: Collection<N>): Boolean {
        TODO("Not yet implemented")
    }

    override fun add(index: Int, element: N) {
        TODO("Not yet implemented")
    }

    override fun add(element: N): Boolean {
        TODO("Not yet implemented")
    }

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

    override fun iterator(): MutableIterator<N> {
        TODO("Not yet implemented")
    }

    override fun lastIndexOf(element: N): Int {
        TODO("Not yet implemented")
    }

    override fun listIterator(): MutableListIterator<N> {
        TODO("Not yet implemented")
    }

    override fun listIterator(index: Int): MutableListIterator<N> {
        TODO("Not yet implemented")
    }

    override fun removeAt(index: Int): N {
        TODO("Not yet implemented")
    }

    override fun set(index: Int, element: N): N {
        TODO("Not yet implemented")
    }

    override fun retainAll(elements: Collection<N>): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(elements: Collection<N>): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(element: N): Boolean {
        TODO("Not yet implemented")
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<N> {
        TODO("Not yet implemented")
    }

    // Sometimes the compiler complains about these methods not being overriden
    fun toArray(): Array<out Any> {
        TODO()
    }

    // Sometimes the compiler complains about these methods not being overriden
    fun <T : Any>toArray(base: Array<out T>): Array<out T> {
        TODO()
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
        expected = expected,
        actual = actual.root!!,
        context = context,
        considerPosition = considerPosition
    )
}

fun assertASTsAreEqual(
    expected: Node,
    actual: Node,
    context: String = "<root>",
    considerPosition: Boolean = false,
    useLightweightAttributeEquality: Boolean = false
) {
    if (expected.nodeType == actual.nodeType) {
        if (considerPosition) {
            assertEquals(expected.position, actual.position, "$context.position")
        }
        expected.originalProperties.forEach { expectedProperty ->
            try {
                val actualProperty = actual.originalProperties.find { it.name == expectedProperty.name }
                    ?: fail("No property ${expectedProperty.name} found at $context")
                val actualPropValue = actualProperty.value
                val expectedPropValue = expectedProperty.value
                if (expectedProperty.provideNodes) {
                    if (expectedProperty.multiple) {
                        if (expectedPropValue is IgnoreChildren<*>) {
                            // Nothing to do
                        } else {
                            val actualPropValueCollection = actualPropValue?.let { it as Collection<Node> }
                            val expectedPropValueCollection = expectedPropValue?.let { it as Collection<Node> }
                            assertEquals(
                                actualPropValueCollection == null,
                                expectedPropValueCollection == null,
                                "$context.${expectedProperty.name} nullness: expected value " +
                                    "to be $expectedPropValueCollection but was $actualPropValueCollection " +
                                    "(node type ${actual.nodeType})"
                            )
                            if (actualPropValueCollection != null && expectedPropValueCollection != null) {
                                assertEquals(
                                    expectedPropValueCollection?.size,
                                    actualPropValueCollection?.size,
                                    "$context.${expectedProperty.name} length"
                                )
                                val expectedIt = expectedPropValueCollection.iterator()
                                val actualIt = actualPropValueCollection.iterator()
                                for (i in expectedPropValueCollection.indices) {
                                    assertASTsAreEqual(
                                        expectedIt.next(),
                                        actualIt.next(),
                                        "$context.${expectedProperty.name}[$i]",
                                        considerPosition = considerPosition,
                                        useLightweightAttributeEquality = useLightweightAttributeEquality
                                    )
                                }
                            }
                        }
                    } else {
                        if (expectedPropValue == null && actualPropValue != null) {
                            assertEquals<Any?>(
                                expectedPropValue,
                                actualPropValue,
                                "$context.${expectedProperty.name}"
                            )
                        } else if (expectedPropValue != null && actualPropValue == null) {
                            assertEquals<Any?>(
                                expectedPropValue,
                                actualPropValue,
                                "$context.${expectedProperty.name}"
                            )
                        } else if (expectedPropValue == null && actualPropValue == null) {
                            // that is ok
                        } else {
                            assertASTsAreEqual(
                                expectedPropValue as Node,
                                actualPropValue as Node,
                                context = "$context.${expectedProperty.name}",
                                considerPosition = considerPosition,
                                useLightweightAttributeEquality = useLightweightAttributeEquality
                            )
                        }
                    }
                } else if (expectedProperty.propertyType == PropertyType.REFERENCE) {
                    if (expectedPropValue is ReferenceByName<*> && actualPropValue is ReferenceByName<*>) {
                        assertEquals(
                            expectedPropValue.name,
                            actualPropValue.name,
                            "$context, comparing reference name of ${expectedProperty.name} of ${expected.nodeType}"
                        )
                        assertEquals(
                            expectedPropValue.referred?.toString(),
                            actualPropValue.referred?.toString(),
                            "$context, comparing reference pointer ${expectedProperty.name} of ${expected.nodeType}"
                        )
                    } else {
                        TODO()
                    }
                } else {
                    if (useLightweightAttributeEquality) {
                        assertEquals(
                            expectedPropValue?.toString(),
                            actualPropValue?.toString(),
                            "$context, comparing property ${expectedProperty.name} of ${expected.nodeType}"
                        )
                    } else {
                        assertEquals(
                            expectedPropValue,
                            actualPropValue,
                            "$context, comparing property ${expectedProperty.name} of ${expected.nodeType}"
                        )
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Issue while processing property $expectedProperty of $expected", e)
            }
        }
    } else {
        fail(
            "$context: expected node of type ${expected.nodeType}, " +
                "but found ${actual.nodeType}"
        )
    }
}

fun Node.assertReferencesResolved(forProperty: KReferenceByName<out Node>) {
    this.kReferenceByNameProperties()
        .filter { it == forProperty }
        .mapNotNull { it.get(this) }
        .forEach {
            val ref = it as ReferenceByName<*>
            assertTrue(
                "Reference $ref in node $this (link ${forProperty.name}) " +
                    "was expected to be solved"
            ) { ref.resolved }
        }
    this.walkChildren().forEach { it.assertReferencesResolved(forProperty = forProperty) }
}

fun Node.assertReferencesResolved(withReturnType: KClass<out PossiblyNamed> = PossiblyNamed::class) {
    this.kReferenceByNameProperties(targetClass = withReturnType)
        .mapNotNull { it.get(this) }
        .forEach {
            assertTrue("Reference $it in node $this at ${this.position} was expected to be solved") {
                (it as ReferenceByName<*>).resolved
            }
        }
    this.walkChildren().forEach { it.assertReferencesResolved(withReturnType = withReturnType) }
}

fun Node.assertReferencesNotResolved(forProperty: KReferenceByName<out Node>) {
    this.kReferenceByNameProperties()
        .filter { it == forProperty }
        .mapNotNull { it.get(this) }
        .forEach { assertFalse { (it as ReferenceByName<*>).resolved } }
    this.walkChildren().forEach { it.assertReferencesNotResolved(forProperty = forProperty) }
}

fun Node.assertReferencesNotResolved(withReturnType: KClass<out PossiblyNamed> = PossiblyNamed::class) {
    this.kReferenceByNameProperties(targetClass = withReturnType)
        .mapNotNull { it.get(this) }
        .forEach { assertFalse { (it as ReferenceByName<*>).resolved } }
    this.walkChildren().forEach { it.assertReferencesNotResolved(withReturnType = withReturnType) }
}
