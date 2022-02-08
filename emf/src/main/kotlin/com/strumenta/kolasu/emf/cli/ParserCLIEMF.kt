package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.strumenta.kolasu.cli.ParsingCommand
import com.strumenta.kolasu.emf.*
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
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
        val language = context.language
        if (language !is EMFMetamodelSupport) {
            System.err.println("The language ${language::class.qualifiedName} does not come with EMF support.")
            exitProcess(1)
        }
        val target = File(output).absolutePath
        val resource = saveMetamodel(language, target)
        super.run()
        val start = System.currentTimeMillis()
        print("Saving AST to $target... ")
        result!!.saveModel(resource, URI.createFileURI(target))
        println("Done (${System.currentTimeMillis() - start}ms).")
    }

    private fun saveMetamodel(language: EMFMetamodelSupport, target: String): Resource {
        val start = System.currentTimeMillis()
        val metamodel = if (this.metamodel != null) {
            File(this.metamodel!!)
        } else {
            File(File(target).parentFile, "metamodel." + (target.substring(target.lastIndexOf(".") + 1)))
        }.absolutePath
        print("Saving metamodel to $metamodel... ")
        val mmResource = language.saveMetamodel(URI.createFileURI(target))
        println("Done (${System.currentTimeMillis() - start}ms).")
        return mmResource
    }

}
