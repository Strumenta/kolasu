package com.strumenta.kolasu.emf

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class KolasuMetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("com.strumenta.kolasu.emf", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()
        ePackage.saveEcore(File("simplemm.ecore"))
        ePackage.saveAsJson(File("simplemm.json"))
        assertEquals("com.strumenta.kolasu.emf", ePackage.name)
        assertEquals(7, ePackage.eClassifiers.size)

//        val cu: EClass = ePackage.eClassifiers.find { it.name == "CompilationUnit" } as EClass
//        assertEquals(1, cu.eAllSuperTypes.size)
//        assertEquals(0, cu.eAllAttributes.size)
//        assertEquals(2, cu.eAllContainments.size)
//        assertEquals(1, cu.eAllReferences.size)
//        assertEquals(1, cu.eAllStructuralFeatures.size)
//
//        val e: EClass = ePackage.eClassifiers.find { it.name == "Expression" } as EClass
//        assertEquals(true, e.isAbstract)
//
//        val sl: EClass = ePackage.eClassifiers.find { it.name == "StringLiteral" } as EClass
//        assertEquals(1, sl.eAllSuperTypes.size)
//        assertEquals(1, sl.eAllAttributes.size)
//        assertEquals(0, sl.eAllContainments.size)
//        assertEquals(0, sl.eAllReferences.size)
//        assertEquals(1, sl.eAllStructuralFeatures.size)
//
//        val vd: EClass = ePackage.eClassifiers.find { it.name == "VarDeclaration" } as EClass
//        assertEquals(2, vd.eAllAttributes.size)
    }
}
