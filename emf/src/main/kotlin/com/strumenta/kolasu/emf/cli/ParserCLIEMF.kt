package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.cli.ASTProcessingCommand
import com.strumenta.kolasu.cli.ParserInstantiator
import com.strumenta.kolasu.emf.*
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import java.io.File


class EMFCommand<R : Node, P : ASTParser<R>, EMFMetamodelSupport>(parserInstantiator: ParserInstantiator<P>) :
    ASTProcessingCommand<R, P>(
            parserInstantiator,
            help = "Parses a file and exports the AST to an EMF (XMI) file.",
            name = "emf"
    ) {
    val outputDirectory by option("--output", "-o")
        .file()
        .help("Directory where to store the output. By default the current directory is used")
        .default(File("."))
    val saveModels by option("--save-models").flag("--no-save-models", "-nsm",default = true)
    val saveMetamodel by option("--metamodel", "-mm").flag(default = false)

    override fun finalizeRun() {
        if (saveMetamodel) {
            this.saveMetamodel()
        }
    }

    override fun processResult(input: File, relativePath: String, result: Result<R>) {
        if (saveModels) {
            val targetFile = File(this.outputDirectory.absolutePath + File.separator + relativePath)
            val targetFileParent = targetFile.parentFile
            targetFileParent.absoluteFile.mkdirs()

            val target = File(output).absolutePath
            val resource = saveMetamodel(language, target)
            super.run()
            result!!.saveModel(resource, URI.createFileURI(target))
        }
    }

    private fun saveMetamodel(language: EMFMetamodelSupport, target: String): Resource {
        val start = System.currentTimeMillis()
        val metamodel = if (this.saveMetamodel != null) {
            File(this.saveMetamodel!!)
        } else {
            File(File(target).parentFile, "metamodel." + (target.substring(target.lastIndexOf(".") + 1)))
        }.absolutePath
        print("Saving metamodel to $metamodel... ")
        val mmResource = language.saveMetamodel(URI.createFileURI(target))
        println("Done (${System.currentTimeMillis() - start}ms).")
        return mmResource
    }
}
