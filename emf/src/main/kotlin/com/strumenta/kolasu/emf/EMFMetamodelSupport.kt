package com.strumenta.kolasu.emf

import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.xmi.XMIResource
import java.io.IOException

interface EMFMetamodelSupport {
    fun generateMetamodel(resource: Resource, includingKolasuMetamodel: Boolean = true)
}

val INCLUDE_KOLASU = EMFMetamodelSupport::class.qualifiedName + ".includeKolasu"
val RESET_URI = EMFMetamodelSupport::class.qualifiedName + ".resetURI"

val DEFAULT_OPTIONS_FOR_METAMODEL = mapOf(
    Pair(XMIResource.OPTION_SCHEMA_LOCATION, true),
    Pair(INCLUDE_KOLASU, false),
    Pair(RESET_URI, true)
)

fun EMFMetamodelSupport.saveMetamodel(
    target: URI,
    options: Map<String, Boolean> = DEFAULT_OPTIONS_FOR_METAMODEL
): Resource {
    val resource =
        createResource(target)
            ?: throw IOException("Unsupported destination: $target")
    this.generateMetamodel(resource, options[INCLUDE_KOLASU] ?: false)
    resource.save(options)
    if (options[RESET_URI] != false) {
        resource.uri = URI.createURI("")
    }
    return resource
}

fun ParsingResult<*>.saveModel(
    metamodelResource: Resource,
    target: URI,
    includeMetamodel: Boolean = true,
    options: Map<String, Boolean>? = null
): Resource {
    val resource =
        metamodelResource.resourceSet.createResource(target)
            ?: throw IOException("Unsupported destination: $target")
    val simplifiedResult = Result(issues, root)
    var eObject: EObject? = null
    eObject = if (includeMetamodel) {
        resource.contents.addAll(metamodelResource.contents)
        simplifiedResult.toEObject(resource)
    } else {
        simplifiedResult.toEObject(metamodelResource)
    }
    resource.contents.add(eObject)
    resource.save(options)
    return resource
}

