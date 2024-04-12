package com.strumenta.kolasu.emf

import com.strumenta.kolasu.ids.Coordinates
import com.strumenta.kolasu.ids.SemanticIDProvider
import com.strumenta.kolasu.model.*
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.parsing.withParseTreeNode
import com.strumenta.kolasu.transformation.GenericNode
import com.strumenta.simplelang.SimpleLangLexer
import com.strumenta.simplelang.SimpleLangParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceImpl
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import org.junit.Test
import java.io.File
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

data class NodeFoo(val name: String) : Node()
class MyRoot(val foo: Int) : Node(), Statement
class MyRootWithIDLogic() : Node(), SemanticIDProvider {
    override fun calculatedID(coordinates: Coordinates): String {
        return ""
    }
}

class MySimpleLangCu() : Node()

class ModelTest {

    @Test
    fun generateSimpleModel() {
        val cu = CompilationUnit(
            listOf(
                VarDeclaration(Visibility.PUBLIC, "a", StringLiteral("foo")),
                VarDeclaration(Visibility.PRIVATE, "b", StringLiteral("bar")),
                VarDeclaration(Visibility.PRIVATE, "c", LocalDateTimeLiteral(LocalDateTime.now()))
            )
        ).withPosition(Position(Point(1, 0), Point(1, 1)))
        val nsURI = "https://strumenta.com/simplemm"
        val metamodelBuilder = MetamodelBuilder(packageName(CompilationUnit::class), nsURI, "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()

        val eo = cu.toEObject(ePackage)
        assertEquals(nsURI, eo.eClass().ePackage.nsURI)
        eo.saveXMI(File("simplemodel.xmi"))
        val jsonFile = File("simplem.json")
        eo.saveAsJson(jsonFile)

        val resourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        // TODO this is to correctly resolve the metamodel, however what would happen if there were
        // other references to https://... resources?
        resourceSet.resourceFactoryRegistry.protocolToFactoryMap["https"] = JsonResourceFactory()
        val kolasuURI = URI.createURI(STARLASU_METAMODEL.nsURI)
        val kolasuRes = resourceSet.createResource(kolasuURI)
        kolasuRes.contents.add(STARLASU_METAMODEL)
        val metaURI = URI.createURI(nsURI)
        val metaRes = resourceSet.createResource(metaURI)
        metaRes.contents.add(ePackage)
        val uri: URI = URI.createFileURI(jsonFile.absolutePath)
        val resource: Resource = resourceSet.createResource(uri)
        assertFalse(resource.isLoaded)
        resource.load(null)
        assertEquals(1, resource.contents.size)
        assertTrue(resource.contents[0] is EObject)
        val eObject = resource.contents[0] as EObject
        val cuClass = ePackage.eClassifiers.find { c -> c.name.equals("CompilationUnit") } as EClass
        assertEquals(cuClass, eObject.eClass())
        val stmts = eObject.eGet(cuClass.getEStructuralFeature("statements")) as EList<*>
        assertEquals(3, stmts.size)
    }

    @Test
    fun generateSimpleModelWithIDLogic() {
        val cu = MyRootWithIDLogic().withPosition(Position(Point(1, 0), Point(1, 1)))
        val nsURI = "https://strumenta.com/simplemmidlogic"
        val metamodelBuilder = MetamodelBuilder(packageName(MyRootWithIDLogic::class), nsURI, "simplemmidlogic")
        metamodelBuilder.provideClass(MyRootWithIDLogic::class)
        assertNotNull(metamodelBuilder.generate())
    }

    @Test
    fun nullCollection() {
        val cu = CompilationUnit(null)
        val nsURI = "https://strumenta.com/simplemm"
        val metamodelBuilder = MetamodelBuilder(packageName(CompilationUnit::class), nsURI, "simplemm")
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()

        val eo = cu.toEObject(ePackage)
        assertEquals(nsURI, eo.eClass().ePackage.nsURI)
        eo.saveXMI(File("simplemodel_null.xmi"))
        val jsonFile = File("simplem_null.json")
        eo.saveAsJson(jsonFile)

        val resourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        // TODO this is to correctly resolve the metamodel, however what would happen if there were
        // other references to https://... resources?
        resourceSet.resourceFactoryRegistry.protocolToFactoryMap["https"] = JsonResourceFactory()
        val metaURI = URI.createURI(nsURI)
        val metaRes = resourceSet.createResource(metaURI)
        metaRes.contents.add(ePackage)
        val uri: URI = URI.createFileURI(jsonFile.absolutePath)
        val resource: Resource = resourceSet.createResource(uri)
        assertFalse(resource.isLoaded)
        resource.load(null)
        assertEquals(1, resource.contents.size)
        assertTrue(resource.contents[0] is EObject)
        val eObject = resource.contents[0] as EObject
        val cuClass = ePackage.eClassifiers.find { c -> c.name.equals("CompilationUnit") } as EClass
        assertEquals(cuClass, eObject.eClass())
        val stmts = eObject.eGet(cuClass.getEStructuralFeature("statements")) as EList<*>
        assertEquals(0, stmts.size)
    }

    @Test
    fun originIsSerialized() {
        val n1 = NodeFoo("abc")
        val n2 = NodeFoo("def").apply {
            origin = n1
        }
        val ePackage = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo")
            .apply {
                provideClass(NodeFoo::class)
            }.generate()
        val mapping = KolasuToEMFMapping()
        val eo1 = n1.toEObject(ePackage, mapping)
        val eo2 = n2.toEObject(ePackage, mapping)
        assertEquals(null, eo1.eGet("origin"))
        assertEquals(true, eo2.eGet("origin") is EObject)
        assertEquals(
            "abc",
            ((eo2.eGet("origin") as EObject).eGet("node") as EObject)
                .eGet("name")
        )
    }

    @Test
    fun destinationIsSerialized() {
        val n1 = NodeFoo("abc").apply {
            destination = TextFileDestination(Position(Point(1, 8), Point(7, 4)))
        }
        val ePackage = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo").apply {
            provideClass(NodeFoo::class)
        }.generate()
        val eo1 = n1.toEObject(ePackage)
        val eo2Destination = eo1.eGet("destination")
        assertEquals(true, eo2Destination is EObject)
        val eo2DestinationEO = eo2Destination as EObject
        assertEquals("TextFileDestination", eo2DestinationEO.eClass().name)
        val textFileDestinationPosition = eo2DestinationEO.eGet("position") as EObject
        assertEquals("Position", textFileDestinationPosition.eClass().name)

        assertEquals(true, textFileDestinationPosition.eGet("start") is EObject)
        val startEO = textFileDestinationPosition.eGet("start") as EObject
        assertEquals(true, textFileDestinationPosition.eGet("end") is EObject)
        val endEO = textFileDestinationPosition.eGet("end") as EObject
        assertEquals(1, startEO.eGet("line"))
        assertEquals(8, startEO.eGet("column"))
        assertEquals(7, endEO.eGet("line"))
        assertEquals(4, endEO.eGet("column"))
    }

    @Test
    fun statementIsConsideredCorrectlyInMetamodel() {
        val mmb = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo")
        mmb.provideClass(MyRoot::class)
        val ePackage = mmb.generate()
        assertEquals(1, ePackage.eClassifiers.size)
        val ec = ePackage.eClassifiers[0] as EClass
        assertEquals("MyRoot", ec.name)
        assertEquals(setOf("ASTNode", "Statement"), ec.eSuperTypes.map { it.name }.toSet())
    }

    @Test
    fun statementIsSerializedCorrectly() {
        val mmb = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo")
        mmb.provideClass(MyRoot::class)
        val ePackage = mmb.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val r1 = MyRoot(124)
        val eo1 = r1.toEObject(res)
        println(eo1.eClass().eSuperTypes)
        assertEquals(setOf("ASTNode", "Statement"), eo1.eClass().eSuperTypes.map { it.name }.toSet())
    }

    @Test
    fun cyclicReferenceByNameOnSingleReference() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        metamodelBuilder.provideClass(NodeWithReference::class)
        val ePackage = metamodelBuilder.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val r1 = NodeWithReference("foo", ReferenceByName("foo"), mutableListOf())
        r1.singlePointer.referred = r1
        val eo1 = r1.toEObject(res)
        val reference = eo1.eGet("singlePointer") as EObject
        assertEquals(STARLASU_METAMODEL.getEClassifier("ReferenceByName"), reference.eClass())
        assertEquals(eo1, reference.eGet("referenced"))
        assertEquals(
            """{
  "eClass" : "#//NodeWithReference",
  "name" : "foo",
  "singlePointer" : {
    "name" : "foo",
    "referenced" : {
      "eClass" : "#//NodeWithReference",
      "${'$'}ref" : "/"
    }
  }
}""".replace("\n", System.lineSeparator()),
            eo1.saveAsJson()
        )
    }

    @Test
    fun cyclicReferenceByNameOnMultipleReference() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        metamodelBuilder.provideClass(NodeWithReference::class)
        val ePackage = metamodelBuilder.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val r1 = NodeWithReference("foo", ReferenceByName("foo"), mutableListOf())
        r1.pointers.add(ReferenceByName("a", r1))
        r1.pointers.add(ReferenceByName("b", r1))
        r1.pointers.add(ReferenceByName("c", r1))
        val eo1 = r1.toEObject(res)

        val pointers = eo1.eGet("pointers") as EList<*>
        assertEquals(3, pointers.size)

        assertEquals(STARLASU_METAMODEL.getEClassifier("ReferenceByName"), (pointers[0] as EObject).eClass())
        assertEquals("a", (pointers[0] as EObject).eGet("name"))
        assertEquals(eo1, (pointers[0] as EObject).eGet("referenced"))

        assertEquals(STARLASU_METAMODEL.getEClassifier("ReferenceByName"), (pointers[1] as EObject).eClass())
        assertEquals("b", (pointers[1] as EObject).eGet("name"))
        assertEquals(eo1, (pointers[1] as EObject).eGet("referenced"))

        assertEquals(STARLASU_METAMODEL.getEClassifier("ReferenceByName"), (pointers[2] as EObject).eClass())
        assertEquals("c", (pointers[2] as EObject).eGet("name"))
        assertEquals(eo1, (pointers[2] as EObject).eGet("referenced"))

        assertEquals(
            """{
  "eClass" : "#//NodeWithReference",
  "name" : "foo",
  "pointers" : [ {
    "name" : "a",
    "referenced" : {
      "eClass" : "#//NodeWithReference",
      "${'$'}ref" : "/"
    }
  }, {
    "name" : "b",
    "referenced" : {
      "eClass" : "#//NodeWithReference",
      "${'$'}ref" : "/"
    }
  }, {
    "name" : "c",
    "referenced" : {
      "eClass" : "#//NodeWithReference",
      "${'$'}ref" : "/"
    }
  } ],
  "singlePointer" : {
    "name" : "foo"
  }
}""".replace("\n", System.lineSeparator()),
            eo1.saveAsJson()
        )
    }

    @Test
    fun forwardAndBackwardReferences() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        metamodelBuilder.provideClass(NodeWithForwardReference::class)
        val ePackage = metamodelBuilder.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val a = NodeWithForwardReference("a", mutableListOf())
        val b = NodeWithForwardReference("b", mutableListOf())
        val c = NodeWithForwardReference("c", mutableListOf())
        val d = NodeWithForwardReference("d", mutableListOf())
        val e = NodeWithForwardReference("e", mutableListOf())

        a.myChildren.add(b)
        a.myChildren.add(c)
        b.myChildren.add(d)
        d.myChildren.add(e)

        a.pointer = ReferenceByName("e", e)
        e.pointer = ReferenceByName("a", a)
        b.pointer = ReferenceByName("c", c)
        c.pointer = ReferenceByName("d", d)
        d.pointer = ReferenceByName("b", b)

        val eoA = a.toEObject(res)

        assertEquals(
            """{
  "eClass" : "#//NodeWithForwardReference",
  "name" : "a",
  "myChildren" : [ {
    "name" : "b",
    "myChildren" : [ {
      "name" : "d",
      "myChildren" : [ {
        "name" : "e",
        "pointer" : {
          "name" : "a",
          "referenced" : {
            "eClass" : "#//NodeWithForwardReference",
            "${'$'}ref" : "/"
          }
        }
      } ],
      "pointer" : {
        "name" : "b",
        "referenced" : {
          "eClass" : "#//NodeWithForwardReference",
          "${'$'}ref" : "//@myChildren.0"
        }
      }
    } ],
    "pointer" : {
      "name" : "c",
      "referenced" : {
        "eClass" : "#//NodeWithForwardReference",
        "${'$'}ref" : "//@myChildren.1"
      }
    }
  }, {
    "name" : "c",
    "pointer" : {
      "name" : "d",
      "referenced" : {
        "eClass" : "#//NodeWithForwardReference",
        "${'$'}ref" : "//@myChildren.0/@myChildren.0"
      }
    }
  } ],
  "pointer" : {
    "name" : "e",
    "referenced" : {
      "eClass" : "#//NodeWithForwardReference",
      "${'$'}ref" : "//@myChildren.0/@myChildren.0/@myChildren.0"
    }
  }
}""".replace("\n", System.lineSeparator()),
            eoA.saveAsJson()
        )
    }

    @Test
    fun saveToJSONWithParseTreeOrigin() {
        // We verify the ParseTreeOrigin is not saved, but the position is
        val pt = SimpleLangParser(CommonTokenStream(SimpleLangLexer(CharStreams.fromString("input A is string"))))
            .compilationUnit()
        val ast = MySimpleLangCu().withParseTreeNode(pt)
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        metamodelBuilder.provideClass(MySimpleLangCu::class)
        val ePackage = metamodelBuilder.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val eo = ast.toEObject(res)
        assertEquals(
            """{
  "eClass" : "#//MySimpleLangCu",
  "position" : {
    "start" : {
      "line" : 1
    },
    "end" : {
      "line" : 1,
      "column" : 17
    }
  }
}""".replace("\n", System.lineSeparator()),
            eo.saveAsJson()
        )
    }

    @Test
    fun saveToJSONNodeOrigin() {
        val someOtherNode = MyRoot(123)
        val ast = MySimpleLangCu().withOrigin(someOtherNode)
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        metamodelBuilder.provideClass(MySimpleLangCu::class)
        metamodelBuilder.provideClass(MyRoot::class)
        val ePackage = metamodelBuilder.generate()

        val res = ResourceImpl()
        res.contents.add(ePackage)
        val mapping = KolasuToEMFMapping()
        val eo1 = someOtherNode.toEObject(res, mapping)
        val eo2 = ast.toEObject(res, mapping)
        assertEquals(
            """{
  "eClass" : "#//MySimpleLangCu",
  "origin" : {
    "eClass" : "https://strumenta.com/starlasu/v2#//NodeOrigin",
    "node" : {
      "eClass" : "#//MyRoot",
      "${'$'}ref" : "#//"
    }
  }
}""".replace("\n", System.lineSeparator()),
            eo2.saveAsJson()
        )
    }

    @Test
    fun handlesGenericNodes() {
        val metamodelBuilder = MetamodelBuilder(
            "com.strumenta.kolasu.emf",
            "https://strumenta.com/simplemm",
            "simplemm"
        )
        val ePackage = metamodelBuilder.generate()
        val res = ResourceImpl()
        res.contents.add(ePackage)
        val mapping = KolasuToEMFMapping()
        val gn = GenericNode().toEObject(res, mapping)
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/v2#//GenericNode"
}""".replace("\n", System.lineSeparator()),
            gn.saveAsJson()
        )
    }
}
