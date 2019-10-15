package com.strumenta.kolasu.model

import java.util.*
import kotlin.test.assertEquals
import org.junit.Test

data class Foo1(val name: String) : Node()
data class Foo2(val names: List<String>) : Node()
data class Foo3(val foo: Foo1) : Node()
data class Foo4(val foos: List<Foo1>?) : Node()

class PropertyDescriptionTest {

    @Test
    fun buildForNotNodeSingleProperty() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo1("gino")
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("name", false, false, "gino"), list[0])
    }

    @Test
    fun buildForNotNodeMultipleProperty() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo2(listOf("gino", "pino"))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("names", false, true, listOf("gino", "pino")), list[0])
    }

    @Test
    fun buildForNodeSingleProperty() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo3(Foo1("gino"))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("foo", true, false, Foo1("gino")), list[0])
    }

    @Test
    fun buildForNodeMultipleProperty() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo4(listOf(Foo1("gino")))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("foos", true, true, listOf(Foo1("gino"))), list[0])
    }

    @Test
    fun buildForNodeMultiplePropertyEmpty() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo4(listOf())
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("foos", true, true, emptyList<Foo1>()), list[0])
    }

    @Test
    fun buildForNodeMultiplePropertyNull() {
        val list = LinkedList<PropertyDescription>()
        val instance = Foo4(null)
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(PropertyDescription("foos", true, true, null), list[0])
    }
}
