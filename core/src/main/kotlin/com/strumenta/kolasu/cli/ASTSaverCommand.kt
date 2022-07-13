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
import java.io.PrintWriter
import java.io.StringWriter

/**
 * This command prints the AST on the console or on file.
 * The formats are the debugging format, JSON, XML, or EMF-JSON.
 */
class ASTSaverCommand<R : Node, P : ASTParser<R>>(parserInstantiator: ParserInstantiator<P>) :
    ASTProcessingCommand<R, P>(
        parserInstantiator,
        help = "Parse files and save the ASTs in the chosen format.",
        name = "ast"
    ) {

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
        echo("A problem prevented from processing ${input.absolutePath}", err = true, trailingNewline = true)
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        e.printStackTrace(pw)
        val stackTraceAsString = sw.toString()
        echo(stackTraceAsString, trailingNewline = true, err = true)
        if (verbose) {
            echo(" FAILURE ${e.message} (${e.javaClass.canonicalName})", trailingNewline = true)
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
                        echo(
                            " -> generating AST for $relativePath (from ${input.absolutePath})",
                            trailingNewline = true
                        )
                    }
                    echo(JsonGenerator().generateString(result), trailingNewline = true)
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.json")
                    if (verbose) {
                        echo(" -> generating ${outputFile.absolutePath}", trailingNewline = true)
                    }
                    JsonGenerator().generateFile(result, outputFile)
                }
            }
            "xml" -> {
                if (print) {
                    if (verbose) {
                        echo(
                            " -> generating AST for $relativePath (from ${input.absolutePath})",
                            trailingNewline = true
                        )
                    }
                    echo(XMLGenerator().generateString(result), trailingNewline = true)
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.xml")
                    if (verbose) {
                        echo(" -> generating ${outputFile.absolutePath}", trailingNewline = true)
                    }
                    XMLGenerator().generateFile(result, outputFile)
                }
            }
            "debug-format" -> {
                if (print) {
                    if (verbose) {
                        echo(
                            " -> generating AST for $relativePath (from ${input.absolutePath})",
                            trailingNewline = true
                        )
                    }
                    echo(result.debugPrint(), trailingNewline = true)
                } else {
                    val outputFile = File(outputDirectory, "${input.name}.df")
                    if (verbose) {
                        echo(" -> generating ${outputFile.absolutePath}", trailingNewline = true)
                    }
                    outputFile.writeText(result.debugPrint())
                }
            }
            else -> throw UnsupportedOperationException()
        }
    }
}
