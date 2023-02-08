package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.*
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.EReference
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

sealed class Statement : Node()
sealed class Expression : Node()
enum class Visibility {
    PUBLIC, PRIVATE
}
class VarDeclaration(var visibility: Visibility, var name: String, var initialValue: Expression) : Statement()
class StringLiteral(var value: String) : Expression()
class LocalDateTimeLiteral(var value: LocalDateTime) : Expression()
data class CompilationUnit(val statements: List<Statement>?) : Node()

@NodeType
interface SomeInterface
data class AltCompilationUnit(val elements: List<SomeInterface>) : Node()

data class NodeWithReference(
    override val name: String,
    val singlePointer: ReferenceByName<NodeWithReference>,
    val pointers: MutableList<ReferenceByName<NodeWithReference>>
) : Node(), Named

data class NodeWithForwardReference(
    override val name: String,
    val myChildren: MutableList<NodeWithForwardReference> = mutableListOf(),
    var pointer: ReferenceByName<NodeWithForwardReference>? = null
) : Node(), Named

data class NodeWithMap(val map: LinkedHashMap<String, StringLiteral> = linkedMapOf()) : Node()

class MetamodelTest {

    private fun temporaryFile(suffix: String): File {
        val f = kotlin.io.path.createTempFile(suffix = suffix).toFile()
        f.deleteOnExit()
        return f
    }

    @Test
    fun generateSimpleMetamodel() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm", "simplemm"
        )
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()

        ePackage.saveEcore(temporaryFile("simplemm.ecore"))
        val jsonFile = temporaryFile("simplemm.json")
        ePackage.saveAsJson(jsonFile)
        checkEPackage(ePackage)

        val resourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.protocolToFactoryMap["https"] = JsonResourceFactory()
        val kolasuURI = URI.createURI(STARLASU_METAMODEL.nsURI)
        val kolasuRes = resourceSet.createResource(kolasuURI)
        kolasuRes.contents.add(STARLASU_METAMODEL)
        val metaURI = URI.createFileURI(jsonFile.absolutePath)
        val metaRes = resourceSet.createResource(metaURI)
        metaRes.load(null)
        assertEquals(1, metaRes.contents.size)
        assertTrue(metaRes.contents[0] is EPackage)
        checkEPackage(metaRes.contents[0] as EPackage)
    }

    private fun checkEPackage(ePackage: EPackage) {
        assertEquals("com.strumenta.kolasu.emf", ePackage.name)
        assertEquals(7, ePackage.eClassifiers.size)

        val cu: EClass = ePackage.eClassifiers.find { it.name == "CompilationUnit" } as EClass
        assertEquals(setOf("ASTNode", "Origin"), cu.eAllGenericSuperTypes.map { it.eClassifier.name }.toSet())
        assertEquals(0, cu.eAllAttributes.size)
        assertEquals(
            setOf("position", "destination", "statements", "origin"),
            cu.eAllContainments.map { it.name }.toSet()
        )
        assertEquals(
            setOf("position", "destination", "origin", "statements"),
            cu.eAllReferences.map {
                it.name
            }.toSet()
        )
        assertEquals(
            setOf("position", "destination", "origin", "statements"),
            cu.eAllStructuralFeatures.map {
                it.name
            }.toSet()
        )

        val e: EClass = ePackage.eClassifiers.find { it.name == "Expression" } as EClass
        assertEquals(true, e.isAbstract)

        val sl: EClass = ePackage.eClassifiers.find { it.name == "StringLiteral" } as EClass
        assertEquals(
            3, sl.eAllSuperTypes.size,
            sl.eAllSuperTypes.joinToString(", ") { it.name }
        )
        assertEquals(1, sl.eSuperTypes.size)
        assertEquals(1, sl.eAttributes.size)
        assertEquals(3, sl.eAllContainments.size, sl.eAllContainments.joinToString(", ") { it.name })
        assertEquals(3, sl.eAllReferences.size, sl.eAllReferences.joinToString(", ") { it.name })
        assertEquals(1, sl.eStructuralFeatures.size)

        val vd: EClass = ePackage.eClassifiers.find { it.name == "VarDeclaration" } as EClass
        assertEquals(2, vd.eAttributes.size)
    }

    @Test
    fun generateSimpleMetamodelWithInterfaces() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm", "simplemm"
        )
        metamodelBuilder.provideClass(AltCompilationUnit::class)
        val ePackage = metamodelBuilder.generate()
        ePackage.saveEcore(File("simplemm.ecore"))
        ePackage.saveAsJson(File("simplemm.json"))
        assertEquals("com.strumenta.kolasu.emf", ePackage.name)
        assertEquals(2, ePackage.eClassifiers.size)

        val AltCompilationUnit: EClass = ePackage.eClassifiers.find { it.name == "AltCompilationUnit" } as EClass
        assertEquals(false, AltCompilationUnit.isInterface)

        val SomeInterface: EClass = ePackage.eClassifiers.find { it.name == "SomeInterface" } as EClass
        assertEquals(true, SomeInterface.isInterface)
    }

    @Test
    fun referenceByName() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm", "simplemm"
        )
        metamodelBuilder.provideClass(NodeWithReference::class)
        val ePackage = metamodelBuilder.generate()
        println(ePackage.saveAsJsonObject().toString())
        assertEquals("com.strumenta.kolasu.emf", ePackage.name)
        assertEquals(1, ePackage.eClassifiers.size)

        val nodeWithReference: EClass = ePackage.eClassifiers.find { it.name == "NodeWithReference" } as EClass
        assertEquals(false, nodeWithReference.isInterface)
        assertEquals(2, nodeWithReference.eStructuralFeatures.size)

        val singlePointer = nodeWithReference.eStructuralFeatures.find { it.name == "singlePointer" } as EReference
        assertEquals(true, singlePointer.isContainment)

        val pointers = nodeWithReference.eStructuralFeatures.find { it.name == "pointers" } as EReference
        assertEquals(true, pointers.isContainment)
    }

    @Test
    fun maps() {
        NodeWithMap::class.processProperties { prop ->
            assertTrue(prop.multiple)
            assertTrue(prop.provideNodes)
        }
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm", "simplemm"
        )
        metamodelBuilder.provideClass(NodeWithMap::class)
        val ePackage = metamodelBuilder.generate()
        assertEquals(
            """{
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "com.strumenta.kolasu.emf",
  "nsURI" : "https://strumenta.com/simplemm",
  "nsPrefix" : "simplemm",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Expression",
    "abstract" : true,
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//ASTNode"
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "LocalDateTimeLiteral",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "//Expression"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "value",
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "https://strumenta.com/starlasu/v2#//LocalDateTime"
      },
      "containment" : true
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "StringLiteral",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "//Expression"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "value",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "StringToStringLiteralMap",
    "instanceClassName" : "java.util.Map${'$'}Entry",
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EAttribute",
      "name" : "key",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
        "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
      }
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "value",
      "lowerBound" : 1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "//StringLiteral"
      }
    } ]
  }, {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "NodeWithMap",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/starlasu/v2#//ASTNode"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "map",
      "upperBound" : -1,
      "eType" : {
        "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
        "${'$'}ref" : "//StringToStringLiteralMap"
      },
      "containment" : true
    } ]
  } ]
}""",
            ePackage.saveAsJson()
        )
    }
}
