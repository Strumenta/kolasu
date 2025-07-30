package com.strumenta.kolasu.emf

import com.strumenta.kolasu.emf.serialization.JsonGenerator
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.pos
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EClass
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ResultTest {

    @Test
    fun generateSimpleModelResult() {
        val cu = CompilationUnit(
            listOf(
                VarDeclaration(Visibility.PUBLIC, "a", StringLiteral("foo")),
                VarDeclaration(Visibility.PRIVATE, "b", StringLiteral("bar"))
            )
        )
        val nsURI = "https://strumenta.com/simplemm"
        val metamodelBuilder = MetamodelBuilder(
            packageName(CompilationUnit::class),
            nsURI,
            "simplemm"
        )
        metamodelBuilder.provideClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()

        val result = Result(
            listOf(
                Issue(IssueType.LEXICAL, "lex", position = Position(Point(1, 1), Point(2, 10))),
                Issue(IssueType.SEMANTIC, "foo")
            ),
            cu
        )

        val eo = result.toEObject(ePackage)
        val emfString = JsonGenerator().generateEMFString(result, ePackage)
        println(emfString)

        val resourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.protocolToFactoryMap["https"] = JsonResourceFactory()
        val kolasuURI = URI.createURI(STARLASU_METAMODEL.nsURI)
        val kolasuRes = resourceSet.createResource(kolasuURI)
        kolasuRes.contents.add(STARLASU_METAMODEL)
        val metaURI = URI.createURI(nsURI)
        val metaRes = resourceSet.createResource(metaURI)
        metaRes.contents.add(ePackage)
        val uri: URI = URI.createURI("file:///emfString.json")
        val resource: Resource = resourceSet.createResource(uri)
        assertFalse(resource.isLoaded)
        emfString.byteInputStream().use {
            resource.load(it, null)
            assertEquals(1, resource.contents.size)
            assertTrue(resource.contents[0] is EObject)
            val resultEO = resource.contents[0] as EObject
            val eObject = resultEO.eGet(resultEO.eClass().getEStructuralFeature("root")) as EObject
            val cuClass = ePackage.eClassifiers.find { c -> c.name.equals("CompilationUnit") } as EClass
            assertEquals(cuClass, eObject.eClass())
            val stmts = eObject.eGet(cuClass.getEStructuralFeature("statements")) as EList<*>
            assertEquals(2, stmts.size)
        }
    }

    @Test
    fun `serialization of issues`() {
        val nsURI = "https://strumenta.com/simplemm"
        val metamodelBuilder = MetamodelBuilder(
            packageName(CompilationUnit::class),
            nsURI,
            "simplemm"
        )
        val ePackage = metamodelBuilder.generate()

        val result: Result<Node> = Result(
            listOf(
                Issue(IssueType.SYNTACTIC, "An error", position = pos(1, 2, 3, 4)),
                Issue(IssueType.LEXICAL, "A warning", severity = IssueSeverity.WARNING),
                Issue(IssueType.SEMANTIC, "An info", severity = IssueSeverity.INFO),
                Issue(IssueType.TRANSLATION, "Translation issue")
            ),
            null
        )
        val emfString = JsonGenerator().generateEMFString(result, ePackage)
        assertEquals(
            """{
  "eClass" : "https://strumenta.com/starlasu/v2#//Result",
  "issues" : [ {
    "type" : "SYNTACTIC",
    "message" : "An error",
    "position" : {
      "start" : {
        "line" : 1,
        "column" : 2
      },
      "end" : {
        "line" : 3,
        "column" : 4
      }
    }
  }, {
    "message" : "A warning",
    "severity" : "WARNING"
  }, {
    "type" : "SEMANTIC",
    "message" : "An info",
    "severity" : "INFO"
  }, {
    "message" : "Translation issue"
  } ]
}""".replace("\n", System.lineSeparator()),
            emfString
        )
    }
}
