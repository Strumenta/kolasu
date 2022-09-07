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
import kotlin.reflect.full.createType
import kotlin.test.assertEquals
import kotlin.test.assertTrue

data class ARoot(val nodes: List<ANodeWithAPair>) : Node()

data class ANodeWithAPair(
    val p: Pair<String, Integer>,
    /*val fieldLocation: Pair<Int, Int>? = Pair(0, 0),*/
) : Node()

class KolasuMetamodelTest {

    private fun temporaryFile(suffix: String): File {
        val f = kotlin.io.path.createTempFile(suffix = suffix).toFile()
        f.deleteOnExit()
        return f
    }

    @Test
    fun handleJavaLangInteger() {
        assertTrue(IntegerHandler.canHandle(java.lang.Integer::class.createType()))
    }

    @Test
    fun generateMetamodelWithGenerics() {
        val jsonFile = temporaryFile("metamodel.json")
        val mmuri = URI.createFileURI(jsonFile.absolutePath)

        val resourceSet: ResourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        val resource =
            resourceSet.createResource(mmuri)
                ?: throw IOException("Unsupported destination: $mmuri")

        val javaIOMetamodelBuilder = MetamodelBuilder(
            "java.io", "https://strumenta.com/javaio", "javaio",
            resource
        )
        javaIOMetamodelBuilder.provideClass(java.io.Serializable::class)
        javaIOMetamodelBuilder.generate()

        val dependencyMetamodelBuilder = MetamodelBuilder(
            "kotlin", "https://strumenta.com/kotlin", "kotlin",
            resource
        )
        dependencyMetamodelBuilder.provideClass(Pair::class)
        dependencyMetamodelBuilder.generate()

        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf", "https://strumenta.com/simplemm", "simplemm",
            resource
        )
        metamodelBuilder.provideClass(ANodeWithAPair::class)
        val ePackage = metamodelBuilder.generate()

        resource.save(null)

        assertEquals(setOf("ANodeWithAPair"), ePackage.eClassifiers.map { it.name }.toSet())

        assertEquals(
            """[ {
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "java.io",
  "nsURI" : "https://strumenta.com/javaio",
  "nsPrefix" : "javaio",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Serializable",
    "abstract" : true,
    "interface" : true
  } ]
}, {
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "kotlin",
  "nsURI" : "https://strumenta.com/kotlin",
  "nsPrefix" : "kotlin",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "Pair",
    "eTypeParameters" : [ {
      "name" : "A"
    }, {
      "name" : "B"
    } ],
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "/0/Serializable"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "first",
      "eGenericType" : {
        "eTypeParameter" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//ETypeParameter",
          "${'$'}ref" : "/1/Pair/A"
        }
      },
      "containment" : true
    }, {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "second",
      "eGenericType" : {
        "eTypeParameter" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//ETypeParameter",
          "${'$'}ref" : "/1/Pair/B"
        }
      },
      "containment" : true
    } ]
  } ]
}, {
  "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EPackage",
  "name" : "com.strumenta.kolasu.emf",
  "nsURI" : "https://strumenta.com/simplemm",
  "nsPrefix" : "simplemm",
  "eClassifiers" : [ {
    "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
    "name" : "ANodeWithAPair",
    "eSuperTypes" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
      "${'$'}ref" : "https://strumenta.com/kolasu/v2#//ASTNode"
    } ],
    "eStructuralFeatures" : [ {
      "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EReference",
      "name" : "p",
      "eGenericType" : {
        "eTypeArguments" : [ {
          "eClassifier" : {
            "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
            "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EString"
          }
        }, {
          "eClassifier" : {
            "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EDataType",
            "${'$'}ref" : "http://www.eclipse.org/emf/2002/Ecore#//EInt"
          }
        } ],
        "eClassifier" : {
          "eClass" : "http://www.eclipse.org/emf/2002/Ecore#//EClass",
          "${'$'}ref" : "/1/Pair"
        }
      },
      "containment" : true
    } ]
  } ]
} ]""",
            jsonFile.readText()
        )
    }
}
