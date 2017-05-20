package me.tomassetti.kolasu.model

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

}