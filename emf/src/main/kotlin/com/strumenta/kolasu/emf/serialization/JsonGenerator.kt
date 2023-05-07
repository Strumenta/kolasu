package com.strumenta.kolasu.emf.serialization

import com.strumenta.kolasu.emf.STARLASU_METAMODEL
import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.ByteArrayOutputStream

class JsonGenerator {
    fun generateEMFString(result: Result<out Node>, astPackage: EPackage): String {
        val uri: URI = URI.createFileURI("dummy-URI.json") ?: throw IllegalStateException("URI not created")
        var resourceSet : ResourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.contentTypeToFactoryMap["application/json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        val resource: Resource = resourceSet.createResource(uri)
        resourceSet = resource.resourceSet ?: throw IllegalStateException("no resource set")
        val starlasuURI = STARLASU_METAMODEL.nsURI ?: throw IllegalStateException("no starlasu URI")
        val starlasuResource = resourceSet.createResource(URI.createURI(starlasuURI), "application/json")
            ?: throw IllegalStateException("starlasuResource is null")
        starlasuResource.contents.add(STARLASU_METAMODEL)
        val pkgResource = resource.resourceSet.createResource(URI.createURI(astPackage.nsURI), "application/json")
        pkgResource.contents.add(astPackage)
        resource.contents.add(result.toEObject(resource))
        val output = ByteArrayOutputStream()
        resource.save(output, null)
        return output.toString(Charsets.UTF_8.name())
    }
}
