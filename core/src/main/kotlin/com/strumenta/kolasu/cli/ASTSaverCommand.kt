package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.debugPrint
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.serialization.JsonGenerator
import com.strumenta.kolasu.serialization.XMLGenerator
import com.strumenta.kolasu.validation.Result
import java.io.File

/**
 * This command prints the AST on the console or on file.
 * The formats are the debugging format, JSON, XML, or EMF-JSON.
 */
abstract class ASTSaverCommand<R : Node, P : ASTParser<R>> : ASTProcessingCommand<R, P>() {

    private val outputFormat by option("--format", "-f")
        .help("Pick the format to serialize ASTs: json (default), xml, json-emf, or debug-format")
        .choice("json", "xml", "debug-format").default("json")
    private val outputDirectory by option("--output", "-o")
        .file()
        .help("Directory where to store the output. By default the current directory is used")
        .default(File("."))
    private val print by option("--print")
        .help("ASTs are not saved on file but they are instead printed on the screen")
        .flag(default = false)

    override fun processException(input: File, relativePath: String, e: Exception) {
        System.err.println("A problem prevented from processing ${input.absolutePath}")
        e.printStackTrace()
        if (verbose) {
            println(" FAILURE ${e.message} (${e.javaClass.canonicalName})")
        }
    }

    override fun processResult(input: File, relativePath: String, result: Result<R>) {
        if (!print) {
            val targetFile = File(this.outputDirectory.absolutePath + File.separator + relativePath)
            val targetFileParent = targetFile.parentFile
            targetFileParent.absoluteFile.mkdirs()
        }
        when (outputFormat) {
            "json" -> {
                if (print) {
                    if (verbose) {
                        println(" -> generating AST for $relativePath (from ${input.absolutePath})")
                    }
                    println(JsonGenerator().generateString(result))
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.json")
                    if (verbose) {
                        println(" -> generating ${outputFile.absolutePath}")
                    }
                    JsonGenerator().generateFile(result, outputFile)
                }
            }
            "xml" -> {
                if (print) {
                    if (verbose) {
                        println(" -> generating AST for $relativePath (from ${input.absolutePath})")
                    }
                    println(XMLGenerator().generateString(result))
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.xml")
                    if (verbose) {
                        println(" -> generating ${outputFile.absolutePath}")
                    }
                    XMLGenerator().generateFile(result, outputFile)
                }
            }
            "debug-format" -> {
                if (print) {
                    if (verbose) {
                        println(" -> generating AST for $relativePath (from ${input.absolutePath})")
                    }
                    println(result.debugPrint())
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.df")
                    if (verbose) {
                        println(" -> generating ${outputFile.absolutePath}")
                    }
                    outputFile.writeText(result.debugPrint())
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }
}
