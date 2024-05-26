package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.explore
import kotlin.test.Test
import kotlin.test.assertEquals

object MyLanguage : StarLasuLanguage("com.foo.a1") {
    init {
        explore(HNodeA::class, HNodeB::class, HNodeC::class, HNodeD::class)
    }
}

@LanguageAssociation(MyLanguage::class)
class HNodeA(
    override var name: String,
) : Node(),
    Named

@LanguageAssociation(MyLanguage::class)
data class HNodeB(
    val nodes: MutableList<HNodeA> = mutableListOf(),
) : Node()

@LanguageAssociation(MyLanguage::class)
data class HNodeC(
    val ref: ReferenceValue<HNodeA>,
) : Node()

@LanguageAssociation(MyLanguage::class)
data class HNodeD(
    var node: HNodeA? = null,
) : Node()

class HomogenousAPITest {
    @Test
    fun attributes() {
        val n1 = HNodeA("foo")
        assertEquals("foo", n1.getAttributeValue("name"))
        n1.setPropertySimpleValue("name", "bar")
        assertEquals("bar", n1.getAttributeValue("name"))
        n1.name = "zum"
        assertEquals("zum", n1.getAttributeValue("name"))
    }

    @Test
    fun multipleContainments() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val n3 = HNodeA("n3")
        val c = HNodeB()
        assertEquals(emptyList(), c.getContainmentValue<HNodeA>("nodes"))
        c.addToContainment("nodes", n1)
        assertEquals(listOf(n1), c.getContainmentValue<HNodeA>("nodes"))
        c.nodes.add(n2)
        c.removeFromContainment("nodes", n1)
        assertEquals(listOf(n2), c.getContainmentValue<HNodeA>("nodes"))
    }

    @Test
    fun singleContainments() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val n3 = HNodeA("n3")
        val c = HNodeD()
        assertEquals(emptyList(), c.getContainmentValue<HNodeA>("node"))
        c.addToContainment("node", n1)
        assertEquals(listOf(n1), c.getContainmentValue<HNodeA>("node"))
        c.node = n2
        assertEquals(listOf(n2), c.getContainmentValue<HNodeA>("node"))
        c.addToContainment("node", n1)
        c.addToContainment("node", n3)
        assertEquals(listOf(n3), c.getContainmentValue<HNodeA>("node"))
        c.removeFromContainment("node", n3)
        assertEquals(listOf(), c.getContainmentValue<HNodeA>("node"))
    }

    @Test
    fun references() {
        val n1 = HNodeA("n1")
        val n2 = HNodeA("n2")
        val c = HNodeC(ReferenceValue("@@@"))
        assertEquals(null, c.getReference<HNodeA>("ref").referred)
        c.setReferenceReferred("ref", n1)
        assertEquals(n1, c.getReference<HNodeA>("ref").referred)
        c.ref.referred = n2
        assertEquals(n2, c.getReference<HNodeA>("ref").referred)
    }
}
