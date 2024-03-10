package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PointTest {
    @Test
    fun rangeWithLength() {
        assertEquals(Range(Point(7, 23), Point(7, 35)), Point(7, 23).rangeWithLength(12))
    }

    @Test
    fun pointToString() {
        assertEquals("Line 7, Column 23", Point(7, 23).toString())
    }

    @Test
    fun pointPlusInt() {
        assertEquals(Point(4, 10), Point(4, 6) + 4)
    }

    @Test
    fun pointPlusEmptyString() {
        assertEquals(Point(7, 23), Point(7, 23) + "")
    }

    @Test
    fun pointPlusString() {
        assertEquals(Point(6, 1), Point(4, 6) + "a\nb\nc")
    }

    @Test
    fun pointPlusStringWindowsNewLines() {
        assertEquals(Point(6, 1), Point(4, 6) + "a\r\nb\r\nc")
    }

    @Test
    fun asRange() {
        assertEquals(Range(Point(5, 3), Point(5, 3)), Point(5, 3).asRange)
    }
}
