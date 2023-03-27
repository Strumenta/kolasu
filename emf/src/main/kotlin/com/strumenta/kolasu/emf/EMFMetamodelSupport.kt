package com.strumenta.kolasu.emf

import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.parsing.KolasuParser
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Result
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.ParserRuleContext
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
    var eObject: EObject?
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

/**
 * A Kolasu parser that supports exporting AST's to EMF/Ecore.
 *
 * In particular, this parser can generate the metamodel. We can then use [ASTNode.toEObject] to translate a tree into
 * its EMF representation.
 */
abstract class EcoreEnabledParser<R : ASTNode, P : Parser, C : ParserRuleContext> :
    KolasuParser<R, P, C>(), EMFMetamodelSupport {

    /**
     * Generates the metamodel. The standard Kolasu metamodel [EPackage][org.eclipse.emf.ecore.EPackage] is included.
     * It does not actually save the resource.
     */
    override fun generateMetamodel(resource: Resource, includingKolasuMetamodel: Boolean) {
        if (includingKolasuMetamodel) {
            resource.contents.add(STARLASU_METAMODEL)
        }
        doGenerateMetamodel(resource)
    }

    /**
     * Implement this method to tell the parser how to generate the metamodel. See [MetamodelBuilder].
     * It does not actually save the resource.
     */
    abstract fun doGenerateMetamodel(resource: Resource)
}
