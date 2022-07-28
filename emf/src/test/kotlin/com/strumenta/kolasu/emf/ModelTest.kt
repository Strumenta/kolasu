package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.Statement
import com.strumenta.kolasu.model.withPosition
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
import kotlin.test.assertTrue

data class NodeFoo(val name: String) : Node()
class MyRoot(val foo: Int) : Node(), Statement

class ModelTest {

    @Test
    fun generateSimpleModel() {
        val cu = CompilationUnit(
            listOf(
                VarDeclaration(Visibility.PUBLIC, "a", StringLiteral("foo")),
                VarDeclaration(Visibility.PRIVATE, "b", StringLiteral("bar")),
                VarDeclaration(Visibility.PRIVATE, "c", LocalDateTimeLiteral(LocalDateTime.now())),
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
        val kolasuURI = URI.createURI(KOLASU_METAMODEL.nsURI)
        val kolasuRes = resourceSet.createResource(kolasuURI)
        kolasuRes.contents.add(KOLASU_METAMODEL)
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
        val ePackage = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo").apply {
            provideClass(NodeFoo::class)
        }.generate()
        val mapping = KolasuToEMFMapping()
        val eo1 = n1.toEObject(ePackage, mapping)
        val eo2 = n2.toEObject(ePackage, mapping)
        assertEquals(null, eo1.eGet("origin"))
        assertEquals(true, eo2.eGet("origin") is EObject)
        assertEquals("abc", (eo2.eGet("origin") as EObject).eGet("name"))
    }

    @Test
    fun destinationIsSerialized() {
        val n1 = NodeFoo("abc").apply {
            destination = Position(Point(1, 8), Point(7, 4))
        }
        val ePackage = MetamodelBuilder("com.strumenta.kolasu.emf", "http://foo.com", "foo").apply {
            provideClass(NodeFoo::class)
        }.generate()
        val eo1 = n1.toEObject(ePackage)
        val eo2Destination = eo1.eGet("destination")
        assertEquals(true, eo2Destination is EObject)
        val eo2DestinationEO = eo2Destination as EObject
        assertEquals("Position", eo2DestinationEO.eClass().name)
        assertEquals(true, eo2DestinationEO.eGet("start") is EObject)
        val startEO = eo2DestinationEO.eGet("start") as EObject
        assertEquals(true, eo2DestinationEO.eGet("end") is EObject)
        val endEO = eo2DestinationEO.eGet("end") as EObject
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
}
