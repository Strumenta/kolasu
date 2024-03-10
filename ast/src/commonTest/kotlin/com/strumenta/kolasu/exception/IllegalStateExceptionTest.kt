package com.strumenta.kolasu.exception

import com.strumenta.kolasu.exceptions.IllegalStateException
import kotlin.test.Test
import kotlin.test.assertEquals

class IllegalStateExceptionTest {
    @Test
    fun message() {
        assertEquals("foo bar", IllegalStateException("foo bar").message)
    }
}
