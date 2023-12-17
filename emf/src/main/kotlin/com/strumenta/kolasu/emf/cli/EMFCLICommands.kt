package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.cli.ASTProcessingCommand
import com.strumenta.kolasu.cli.ParserInstantiator
import com.strumenta.kolasu.cli.changeExtension
import com.strumenta.kolasu.emf.EMFMetamodelSupport
import com.strumenta.kolasu.emf.EcoreEnabledParser
import com.strumenta.kolasu.emf.createResource
import com.strumenta.kolasu.emf.saveMetamodel
import com.strumenta.kolasu.emf.saveModel
import com.strumenta.kolasu.model.INode
import com.strumenta.kolasu.parsing.ParsingResult
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import java.io.File

class EMFModelCommand<R : INode, P>(parserInstantiator: ParserInstantiator<P>) :
    ASTProcessingCommand<R, P>(
        parserInstantiator,
        help = "Parses a file and exports the AST to an EMF (XMI) file.",
        name = "model"
    ) where P : EcoreEnabledParser<R, *, *, *> {
    val metamodel by option("--metamodel")
    val outputDirectory by option("--output", "-o")
        .file()
        .help("Directory where to store the output. By default the current directory is used.")
        .default(File("."))
    val includeMetamodel by option("--include-metamodel", "-imm")
        .help("Include metamodel in each saved model.")
        .flag(default = false)

    override fun finalizeRun() {
        // Nothing to do
    }

    override fun processException(input: File, relativePath: String, e: Exception) {
        // Nothing to do
    }

    override fun processResult(input: File, relativePath: String, result: ParsingResult<R>, parser: P) {
        val targetFile = File(this.outputDirectory.absolutePath + File.separator + relativePath)
            .changeExtension("json")
        val targetFileParent = targetFile.parentFile
        targetFileParent.absoluteFile.mkdirs()

        val mmFile = File("metamodel.json")

        val resource = saveMetamodel(parser, mmFile)
        val start = System.currentTimeMillis()
        if (verbose) {
            echo("Saving AST for $input to $targetFile... ")
        }
        result.saveModel(resource, URI.createFileURI(targetFile.path), includeMetamodel = includeMetamodel)
        if (verbose) {
            echo("Done (${System.currentTimeMillis() - start}ms).")
        }
    }

    private fun saveMetamodel(parser: EMFMetamodelSupport, target: File): Resource {
        val start = System.currentTimeMillis()
        val metamodel = if (this.metamodel != null) {
            File(this.metamodel!!)
        } else {
            File(target.parentFile, "metamodel." + (target.path.substring(target.path.lastIndexOf(".") + 1)))
        }.absolutePath
        if (verbose) {
            echo("Saving metamodel to $metamodel... ")
        }
        val mmResource = parser.saveMetamodel(URI.createFileURI(target.path))
        if (verbose) {
            echo("Done (${System.currentTimeMillis() - start}ms).")
        }
        return mmResource
    }
}

class EMFMetaModelCommand(val metamodelSupport: EMFMetamodelSupport) :
    CliktCommand(
        help = "Generate the metamodel for a language.",
        name = "metamodel"
    ) {
    val verbose by option("--verbose", "-v")
        .help("Print additional messages")
        .flag(default = false)
    val output by option("--output", "-o")
        .file()
        .help("File where to store the metamodel")
        .default(File("metamodel.json"))
    val includeKolasu by option("--include-kolasu", "-ik").flag(default = false)

    override fun run() {
        val mmResource = createResource(URI.createFileURI(output.path))!!
        metamodelSupport.generateMetamodel(mmResource, includeKolasu)
        mmResource.save(emptyMap<Any, Any>())
        if (verbose) {
            echo("Metamodel saved to ${output.absolutePath}")
        }
    }
}
