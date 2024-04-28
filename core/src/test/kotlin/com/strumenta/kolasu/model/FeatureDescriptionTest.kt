package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Attribute
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import org.junit.Test
import java.util.LinkedList
import kotlin.test.assertEquals
import kotlin.test.assertFalse

object MyLanguage4 : StarLasuLanguage("com.foo.language4") {
    init {
        explore(
            Foo1::class,
            Foo3::class,
            Foo4::class,
        )
    }
}

@LanguageAssociation(MyLanguage4::class)
data class Foo1(
    val name: String,
) : Node()

@LanguageAssociation(MyLanguage4::class)
data class Foo3(
    val foo: Foo1,
) : Node()

@LanguageAssociation(MyLanguage4::class)
data class Foo4(
    val foos: List<Foo1>,
) : Node()

class FeatureDescriptionTest {
    @Test
    fun buildForNotNodeSingleProperty() {
        val list = LinkedList<Pair<Feature, NodeLike>>()
        val instance = Foo1("gino")
        instance.processFeatures { it, node ->
            list.add(Pair(it, node))
        }
        assertEquals(1, list.size)
        val f = list[0].first
        val value = f.value(list[0].second)
        assertEquals("name", f.name)
        assertEquals(Multiplicity.SINGULAR, f.multiplicity)
        assertEquals("gino", value)
        assert(f is Attribute)
        assertFalse(f.derived)
    }

    @Test
    fun buildForNodeSingleProperty() {
        val list = LinkedList<Pair<Feature, NodeLike>>()
        val instance = Foo3(Foo1("gino"))
        instance.processFeatures { it, node ->
            list.add(Pair(it, node))
        }
        assertEquals(1, list.size)
        val f = list[0].first
        val value = f.value(list[0].second)
        assertEquals("foo", f.name)
        assertEquals(Multiplicity.SINGULAR, f.multiplicity)
        assertEquals(Foo1("gino"), value)
        assert(f is Containment)
        assertFalse(f.derived)
    }

    @Test
    fun buildForNodeMultipleProperty() {
        val list = LinkedList<Pair<Feature, NodeLike>>()
        val instance = Foo4(listOf(Foo1("gino")))
        instance.processFeatures { it, node ->
            list.add(Pair(it, node))
        }
        assertEquals(1, list.size)
        val f = list[0].first
        val value = f.value(list[0].second)
        assertEquals("foos", f.name)
        assertEquals(Multiplicity.MANY, f.multiplicity)
        assertEquals(listOf(Foo1("gino")), value)
        assert(f is Containment)
        assertFalse(f.derived)
    }

    @Test
    fun buildForNodeMultiplePropertyEmpty() {
        val list = LinkedList<Pair<Feature, NodeLike>>()
        val instance = Foo4(listOf())
        instance.processFeatures { it, node ->
            list.add(Pair(it, node))
        }
        assertEquals(1, list.size)
        val f = list[0].first
        val value = f.value(list[0].second)
        assertEquals("foos", f.name)
        assertEquals(Multiplicity.MANY, f.multiplicity)
        assertEquals(emptyList<NodeLike>(), value)
        assert(f is Containment)
        assertFalse(f.derived)
    }
}
