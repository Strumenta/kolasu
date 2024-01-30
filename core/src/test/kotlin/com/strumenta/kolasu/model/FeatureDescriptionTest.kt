package com.strumenta.kolasu.model

import org.junit.Test
import java.util.LinkedList
import kotlin.test.assertEquals

data class Foo1(
    val name: String,
) : Node()

data class Foo2(
    val names: List<String>,
) : Node()

data class Foo3(
    val foo: Foo1,
) : Node()

data class Foo4(
    val foos: List<Foo1>?,
) : Node()

class FeatureDescriptionTest {
    @Test
    fun buildForNotNodeSingleProperty() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo1("gino")
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "name",
                false,
                Multiplicity.SINGULAR,
                "gino",
                FeatureType.ATTRIBUTE,
            ),
            list[0],
        )
    }

    @Test
    fun buildForNotNodeMultipleProperty() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo2(listOf("gino", "pino"))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "names",
                false,
                Multiplicity.MANY,
                listOf("gino", "pino"),
                FeatureType.ATTRIBUTE,
            ),
            list[0],
        )
    }

    @Test
    fun buildForNodeSingleProperty() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo3(Foo1("gino"))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "foo",
                true,
                Multiplicity.SINGULAR,
                Foo1("gino"),
                FeatureType.CONTAINMENT,
            ),
            list[0],
        )
    }

    @Test
    fun buildForNodeMultipleProperty() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo4(listOf(Foo1("gino")))
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "foos",
                true,
                Multiplicity.MANY,
                listOf(Foo1("gino")),
                FeatureType.CONTAINMENT,
            ),
            list[0],
        )
    }

    @Test
    fun buildForNodeMultiplePropertyEmpty() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo4(listOf())
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "foos",
                true,
                Multiplicity.MANY,
                emptyList<Foo1>(),
                FeatureType.CONTAINMENT,
            ),
            list[0],
        )
    }

    @Test
    fun buildForNodeMultiplePropertyNull() {
        val list = LinkedList<FeatureDescription>()
        val instance = Foo4(null)
        instance.processProperties {
            list.add(it)
        }
        assertEquals(1, list.size)
        assertEquals(
            FeatureDescription(
                "foos",
                true,
                Multiplicity.MANY,
                null,
                FeatureType.CONTAINMENT,
            ),
            list[0],
        )
    }
}
