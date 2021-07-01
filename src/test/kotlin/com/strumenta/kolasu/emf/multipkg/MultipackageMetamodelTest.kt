package com.strumenta.kolasu.emf.multipkg

import com.strumenta.kolasu.emf.CompilationUnit
import com.strumenta.kolasu.emf.MetamodelBuilder
import org.eclipse.emf.common.util.URI
import org.emfjson.jackson.resource.JsonResourceFactory
import java.io.File
import kotlin.test.assertEquals
import org.junit.Test

data class MultiCU(val cus: List<CompilationUnit>)

class MultipackageMetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val resource = JsonResourceFactory().createResource(URI.createFileURI("multipkg.json"))

        val mb1 = MetamodelBuilder("com.strumenta.kolasu.emf", "https://strumenta.com/simplemm", "simplemm", resource)
        mb1.provideClass(CompilationUnit::class)
        assertEquals(6, mb1.generate().eClassifiers.size)

        val mb2 = MetamodelBuilder("com.strumenta.kolasu.emf.multipkg", "https://strumenta.com/simplemm2", "simplemm2", resource)
        mb2.provideClass(MultiCU::class)
        assertEquals(1, mb2.generate().eClassifiers.size)

        resource.save(null)

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
