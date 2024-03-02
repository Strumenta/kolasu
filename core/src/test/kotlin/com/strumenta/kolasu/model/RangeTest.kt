package com.strumenta.kolasu.model

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test as test

data class MySetStatement(
    val specifiedRange: Range? = null,
) : Node(specifiedRange)

class RangeTest {
    @test
    fun containsNode() {
        val before = Node(Range(Point(1, 0), Point(1, 10)))
        val inside = Node(Range(Point(2, 3), Point(2, 8)))
        val after = Node(Range(Point(3, 0), Point(3, 10)))
        val range = Range(Point(2, 0), Point(2, 10))

        assertFalse("contains should return false with node before") { range.contains(before.range) }
        assertTrue("contains should return true with node inside") { range.contains(inside.range) }
        assertFalse("contains should return false with node after") { range.contains(after.range) }
    }

    @test(expected = Exception::class)
    fun illegalRangeNotAccepted() {
        Range(Point(10, 1), Point(5, 2), validate = true)
    }
}
