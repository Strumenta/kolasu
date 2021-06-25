package com.strumenta.kolasu.emf.simple

import com.strumenta.kolasu.emf.KOLASU_METAMODEL
import com.strumenta.kolasu.emf.MetamodelBuilder
import com.strumenta.kolasu.emf.getEClass
import org.junit.Test
import kotlin.test.assertEquals

class MetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("Simple2MM", "https://strumenta.com/simplemm2", "simplemm2")
        metamodelBuilder.provideClass(DataDefinition::class)
        val ePackage = metamodelBuilder.generate()

        val dataDefinition = ePackage.getEClass("DataDefinition")
        val abstractDataDefinition = ePackage.getEClass("AbstractDataDefinition")

        assertEquals(2, abstractDataDefinition.eSuperTypes.size)
        val abstractDataDefinitionInterfaces = abstractDataDefinition.eSuperTypes.filter { it.isInterface }
        assertEquals(1, abstractDataDefinitionInterfaces.size)
        assertEquals(KOLASU_METAMODEL.getEClassifier("Named"), abstractDataDefinitionInterfaces[0])
        assertEquals(0, abstractDataDefinition.eAttributes.size)
        assertEquals(1, abstractDataDefinition.eReferences.size)
    }
}