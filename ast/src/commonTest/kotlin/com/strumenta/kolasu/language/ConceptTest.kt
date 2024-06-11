package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.Multiplicity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ConceptTest {
    @Test
    fun testAllContainments() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals(3, c.allContainments.size)
        assertEquals(setOf("MyCont", "MyCont2", "MyCont3"), c.allContainments.map { it.name }.toSet())
    }

    @Test
    fun testAllReferences() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals(1, c.allReferences.size)
        assertEquals(setOf("MyRef"), c.allReferences.map { it.name }.toSet())
    }

    @Test
    fun testFeature() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals("MyProp", c.feature("MyProp")?.name)
        assertEquals("MyProp2", c.feature("MyProp2")?.name)
        assertEquals("MyRef", c.feature("MyRef")?.name)
        assertEquals("MyCont3", c.feature("MyCont3")?.name)
        assertEquals(null, c.feature("Unexisting")?.name)
    }

    @Test
    fun testProperty() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals("MyProp", c.property("MyProp")?.name)
        assertEquals("MyProp2", c.property("MyProp2")?.name)
        assertEquals(null, c.property("MyRef")?.name)
        assertEquals(null, c.property("MyCont3")?.name)
        assertEquals(null, c.property("Unexisting")?.name)
    }

    @Test
    fun testContainment() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals(null, c.containment("MyProp")?.name)
        assertEquals(null, c.containment("MyProp2")?.name)
        assertEquals(null, c.containment("MyRef")?.name)
        assertEquals("MyCont3", c.containment("MyCont3")?.name)
        assertEquals(null, c.containment("Unexisting")?.name)
    }

    @Test
    fun testReference() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals(null, c.reference("MyProp")?.name)
        assertEquals(null, c.reference("MyProp2")?.name)
        assertEquals("MyRef", c.reference("MyRef")?.name)
        assertEquals(null, c.reference("MyCont3")?.name)
        assertEquals(null, c.feature("Unexisting")?.name)
    }

    @Test
    fun testRequireProperty() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals("MyProp", c.requireProperty("MyProp")?.name)
        assertEquals("MyProp2", c.requireProperty("MyProp2")?.name)
        assertFailsWith<IllegalArgumentException> { c.requireProperty("MyCont") }
        assertFailsWith<IllegalArgumentException> { c.requireProperty("Unexisting") }
    }

    @Test
    fun testRequireContainment() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals("MyCont", c.requireContainment("MyCont")?.name)
        assertFailsWith<IllegalArgumentException> { c.requireContainment("MyProp") }
        assertFailsWith<IllegalArgumentException> { c.requireContainment("Unexisting") }
    }

    @Test
    fun testRequireReference() {
        val l = StarLasuLanguage("my.language")
        val sc = Concept(l, "SC")
        val i1 = ConceptInterface(l, "I1")
        val i2 = ConceptInterface(l, "I2")
        val c = Concept(l, "C")
        c.superConcept = sc
        c.conceptInterfaces.add(i1)
        c.conceptInterfaces.add(i2)
        i1.declaredFeatures.add(Property("MyProp", false, stringType, { TODO() }))
        i2.declaredFeatures.add(Property("MyProp2", false, stringType, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont", Multiplicity.MANY, c, { TODO() }))
        c.declaredFeatures.add(Reference("MyRef", false, c, { TODO() }))
        c.declaredFeatures.add(Containment("MyCont2", Multiplicity.MANY, c, { TODO() }))
        sc.declaredFeatures.add(Containment("MyCont3", Multiplicity.MANY, c, { TODO() }))

        assertEquals("MyRef", c.requireReference("MyRef")?.name)
        assertFailsWith<IllegalArgumentException> { c.requireReference("MyProp") }
        assertFailsWith<IllegalArgumentException> { c.requireReference("Unexisting") }
    }
}
