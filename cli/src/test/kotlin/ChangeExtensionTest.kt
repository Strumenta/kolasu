package com.strumenta.kolasu.cli

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class ChangeExtensionTest {
    @Test
    fun noExtension() {
        assertEquals(File("a.xml"), File("a").changeExtension("xml"))
    }

    @Test
    fun noPrefix() {
        assertEquals(File("a.xml"), File("a.json").changeExtension("xml"))
    }

    @Test
    fun withPrefix() {
        assertEquals(File("foo/bar/a.xml"), File("foo/bar/a.json").changeExtension("xml"))
    }
}
