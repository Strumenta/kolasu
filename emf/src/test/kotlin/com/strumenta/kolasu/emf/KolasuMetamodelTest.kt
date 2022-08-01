package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import org.junit.Test
import java.io.File
import java.io.IOException
import kotlin.test.assertEquals


data class ARoot(val nodes: List<ANodeWithAPair>) : Node()

data class ANodeWithAPair(val p : Pair<String, Integer>,
                          /*val fieldLocation: Pair<Int, Int>? = Pair(0, 0),*/) : Node()

class KolasuMetamodelTest {

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()
        ePackage.saveEcore(File("simplemm.ecore"))
        ePackage.saveAsJson(File("simplemm.json"))
        assertEquals("SimpleMM", ePackage.name)
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

    @Test
    fun generateMetamodelWithGenerics() {
        val metamodelBuilder = MetamodelBuilder("SimpleMM", "https://strumenta.com/simplemm", "simplemm")
        metamodelBuilder.provideClass(ANodeWithAPair::class)
        val ePackage = metamodelBuilder.generate()

        val jsonFile = kotlin.io.path.createTempFile(suffix = "metamodel.json").toFile()
        jsonFile.deleteOnExit()
        val mmuri = URI.createFileURI(jsonFile.absolutePath)
        val resourceSet: ResourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        val resource =
            resourceSet.createResource(mmuri)
                ?: throw IOException("Unsupported destination: $mmuri")
        resource.contents.add(ePackage)
        resource.save(null)
        println(jsonFile.readText())
    }
}
