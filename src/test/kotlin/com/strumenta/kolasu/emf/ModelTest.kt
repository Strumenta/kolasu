package com.strumenta.kolasu.emf

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.emfjson.jackson.resource.JsonResourceFactory
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import kotlin.test.assertEquals

class ModelTest {

    @Test
    fun generateSimpleModel() {
        val cu = CompilationUnit(listOf(
                VarDeclaration("a", StringLiteral("foo")),
                VarDeclaration("b", StringLiteral("bar"))
        ))
        val nsURI = "https://strumenta.com/simplemm"
        val metamodelBuilder = MetamodelBuilder("SimpleMM", nsURI)
        metamodelBuilder.addClass(CompilationUnit::class)
        val ePackage = metamodelBuilder.generate()

        val eo = cu.toEObject(ePackage)
        assertEquals(nsURI, eo.eClass().ePackage.nsURI)
        eo.saveXMI(File("simplemodel.xmi"))
        val jsonFile = File("simplem.json")
        eo.saveAsJson(jsonFile)

        val resourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        val uri: URI = URI.createFileURI(jsonFile.absolutePath)
        val resource: Resource = resourceSet.createResource(uri)
        resource.contents.add(ePackage)
        //TODO this fails
        //resource.load(null)
        //assertEquals(1, resource.contents.size)
    }
}