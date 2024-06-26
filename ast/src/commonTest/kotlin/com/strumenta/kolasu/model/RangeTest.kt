package com.strumenta.kolasu.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RangeTest {
    @Test
    fun offset() {
        val code =
            """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals(0, START_POINT.offset(code))
        assertEquals(5, Point(1, 5).offset(code))
        assertEquals(17, Point(1, 17).offset(code))
        assertEquals(18, Point(2, 0).offset(code))
        assertFails { Point(1, 18).offset(code) }
        assertFails { Point(4, 0).offset(code) }
    }

    @Test
    fun pointCompare() {
        val p0 = START_POINT
        val p1 = Point(1, 1)
        val p2 = Point(1, 100)
        val p3 = Point(2, 90)

        assertEquals(false, p0 < p0)
        assertEquals(true, p0 <= p0)
        assertEquals(true, p0 >= p0)
        assertEquals(false, p0 > p0)

        assertEquals(true, p0 < p1)
        assertEquals(true, p0 <= p1)
        assertEquals(false, p0 >= p1)
        assertEquals(false, p0 > p1)

        assertEquals(true, p0 < p2)
        assertEquals(true, p0 <= p2)
        assertEquals(false, p0 >= p2)
        assertEquals(false, p0 > p2)

        assertEquals(true, p0 < p3)
        assertEquals(true, p0 <= p3)
        assertEquals(false, p0 >= p3)
        assertEquals(false, p0 > p3)

        assertEquals(true, p1 < p2)
        assertEquals(true, p1 <= p2)
        assertEquals(false, p1 >= p2)
        assertEquals(false, p1 > p2)

        assertEquals(true, p1 < p3)
        assertEquals(true, p1 <= p3)
        assertEquals(false, p1 >= p3)
        assertEquals(false, p1 > p3)
    }

    @Test
    fun isBefore() {
        val p0 = START_POINT
        val p1 = Point(1, 1)
        val p2 = Point(1, 100)
        val p3 = Point(2, 90)

        assertEquals(false, p0.isBefore(p0))
        assertEquals(true, p0.isBefore(p1))
        assertEquals(true, p0.isBefore(p2))
        assertEquals(true, p0.isBefore(p3))

        assertEquals(false, p1.isBefore(p0))
        assertEquals(false, p1.isBefore(p1))
        assertEquals(true, p1.isBefore(p2))
        assertEquals(true, p1.isBefore(p3))

        assertEquals(false, p2.isBefore(p0))
        assertEquals(false, p2.isBefore(p1))
        assertEquals(false, p2.isBefore(p2))
        assertEquals(true, p2.isBefore(p3))

        assertEquals(false, p3.isBefore(p0))
        assertEquals(false, p3.isBefore(p1))
        assertEquals(false, p3.isBefore(p2))
        assertEquals(false, p3.isBefore(p3))
    }

    @Test
    fun text() {
        val code =
            """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals("", Range(START_POINT, START_POINT).text(code))
        assertEquals("t", Range(START_POINT, Point(1, 1)).text(code))
        assertEquals("this is some cod", Range(START_POINT, Point(1, 16)).text(code))
        assertEquals("this is some code", Range(START_POINT, Point(1, 17)).text(code))
        assertEquals("this is some code\n", Range(START_POINT, Point(2, 0)).text(code))
        assertEquals("this is some code\ns", Range(START_POINT, Point(2, 1)).text(code))
    }

    @Test
    fun containsPoint() {
        val before = Point(1, 0)
        val start = Point(1, 1)
        val middle = Point(1, 2)
        val end = Point(1, 3)
        val after = Point(1, 4)
        val range = Range(start, end)

        assertFalse("contains should return false with point before") { range.contains(before) }
        assertTrue("contains should return true with point at the beginning") { range.contains(start) }
        assertTrue("contains should return true with point in the middle") { range.contains(middle) }
        assertTrue("contains should return true with point at the end") { range.contains(end) }
        assertFalse("contains should return false with point after") { range.contains(after) }
    }

    @Test
    fun containsRange() {
        val before = Range(Point(1, 0), Point(1, 10))
        val inside = Range(Point(2, 3), Point(2, 8))
        val after = Range(Point(3, 0), Point(3, 10))
        val range = Range(Point(2, 0), Point(2, 10))

        assertFalse("contains should return false with range before") { range.contains(before) }
        assertTrue("contains should return true with same range") { range.contains(range) }
        assertTrue("contains should return true with range inside") { range.contains(inside) }
        assertFalse("contains should return false with range after") { range.contains(after) }
    }

    @Test
    fun illegalRangeAccepted() {
        Range(Point(10, 1), Point(5, 2), validate = false)
    }
}
