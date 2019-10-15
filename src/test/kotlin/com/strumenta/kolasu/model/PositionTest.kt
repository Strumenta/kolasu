package com.strumenta.kolasu.model

import kotlin.test.assertEquals
import kotlin.test.assertFails
import org.junit.Test as test

class PositionTest {

    @test fun offset() {
        val code = """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals(0, START_POINT.offset(code))
        assertEquals(5, Point(1, 5).offset(code))
        assertEquals(17, Point(1, 17).offset(code))
        assertEquals(18, Point(2, 0).offset(code))
        assertFails { Point(1, 18).offset(code) }
        assertFails { Point(4, 0).offset(code) }
    }

    @test fun pointCompare() {
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

    @test fun isBefore() {
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

    @test fun text() {
        val code = """this is some code
                     |second line
                     |third line""".trimMargin("|")
        assertEquals("", Position(START_POINT, START_POINT).text(code))
        assertEquals("t", Position(START_POINT, Point(1, 1)).text(code))
        assertEquals("this is some cod", Position(START_POINT, Point(1, 16)).text(code))
        assertEquals("this is some code", Position(START_POINT, Point(1, 17)).text(code))
        assertEquals("this is some code\n", Position(START_POINT, Point(2, 0)).text(code))
        assertEquals("this is some code\ns", Position(START_POINT, Point(2, 1)).text(code))
    }
}
