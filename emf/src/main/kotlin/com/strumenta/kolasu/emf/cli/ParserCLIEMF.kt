package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.strumenta.kolasu.cli.ParsingCommand
import com.strumenta.kolasu.emf.EMFEnabledParser
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.ResourceSet
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.XMIResource
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl
import org.emfjson.jackson.resource.JsonResourceFactory
import java.io.File
import java.io.IOException
import kotlin.system.exitProcess

open class ParserCLIEMF<R : Node>(
    help: String = "Parses a file and exports the AST to an EMF (XMI) file.",
    epilog: String = "",
    name: String? = "emf",
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap(),
    autoCompleteEnvvar: String? = "",
    allowMultipleSubcommands: Boolean = false,
    treatUnknownOptionsAsArgs: Boolean = false,
    exitOnFail: Boolean = true,
    printASTSummary: Boolean = true
) : ParsingCommand<R>(
    help, epilog, name, invokeWithoutSubcommand, printHelpOnEmptyArgs, helpTags, autoCompleteEnvvar,
    allowMultipleSubcommands, treatUnknownOptionsAsArgs, exitOnFail, printASTSummary
) {
    val output by argument()
    val metamodel by option("--metamodel")
    override fun run() {
        if (context.language !is EMFEnabledParser) {
            System.err.println("The language ${context.language::class.qualifiedName} does not come with EMF support.")
            exitProcess(1)
        }
        val resourceSet: ResourceSet = ResourceSetImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["json"] = JsonResourceFactory()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["xmi"] = XMIResourceFactoryImpl()
        resourceSet.resourceFactoryRegistry.extensionToFactoryMap["ecore"] = EcoreResourceFactoryImpl()
        val options = mapOf(Pair(XMIResource.OPTION_SCHEMA_LOCATION, true))
        val target = File(output).absolutePath
        val mmResource = saveMetamodel(target, resourceSet, options)
        super.run()
        saveModel(target, resourceSet, mmResource, options)
    }

    private fun saveModel(
        target: String,
        resourceSet: ResourceSet,
        mmResource: Resource,
        options: Map<String, Boolean>
    ) {
        val simplifiedResult = Result(result!!.issues, result!!.root)
        val start = System.currentTimeMillis()
        print("Saving AST to $target... ")
        val resource =
            resourceSet.createResource(URI.createFileURI(target))
                ?: throw IOException("Unsupported destination: $target")
        val eObject = simplifiedResult.toEObject(mmResource)
        resource.contents.add(eObject)
        resource.save(options)
        println("Done (${System.currentTimeMillis() - start}ms).")
    }

    private fun saveMetamodel(target: String, resourceSet: ResourceSet, options: Map<String, Boolean>): Resource {
        val start = System.currentTimeMillis()
        val metamodel = if (this.metamodel != null) {
            File(this.metamodel)
        } else {
            File(File(target).parentFile, "metamodel." + (target.substring(target.lastIndexOf(".") + 1)))
        }.absolutePath
        print("Saving metamodel to $metamodel... ")
        val resource =
            resourceSet.createResource(URI.createFileURI(metamodel))
                ?: throw IOException("Unsupported destination: $metamodel")
        (context.language as EMFEnabledParser).generateMetamodel(resource)
        resource.save(options)
        println("Done (${System.currentTimeMillis() - start}ms).")
        return resource
    }
}
