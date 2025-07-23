package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.IssueSeverity
import java.io.File
import java.nio.charset.Charset
import java.util.function.Function
import kotlin.system.exitProcess

typealias ParserInstantiator<P> = Function<File, P?>

abstract class ASTProcessingCommand<R : Node, P : ASTParser<R>>(
    val parserInstantiator: ParserInstantiator<P>,
    help: String = "",
    name: String? = null
) :
    CliktCommand(help = help, name = name) {
    protected val inputs by argument().file(mustExist = true).multiple()

    protected val charset by option("--charset", "-c")
        .help("Set the charset to use to load the files. Default is UTF-8")
        .default("UTF-8")
    protected val ignorePositions by option("--ignore-positions")
        .help("Ignore positions, so that they do not appear in the AST")
        .flag("--consider-positions", default = false)
    protected val verbose by option("--verbose", "-v")
        .help("Print additional messages")
        .flag(default = false)

    override fun run() {
        initializeRun()
        if (inputs.isEmpty()) {
            echo("No inputs specified, exiting", trailingNewline = true)
            exitProcess(1)
        }
        inputs.forEach { processInput(it, explicit = true, relativePath = "") }
        finalizeRun()
    }

    protected open fun initializeRun() {
    }

    protected open fun finalizeRun() {
    }

    /**
     * If null is returned it means we cannot parse this file
     */
    private fun instantiateParser(input: File): P? {
        return parserInstantiator.apply(input)
    }

    protected abstract fun processResult(input: File, relativePath: String, result: ParsingResult<R>, parser: P)
    protected abstract fun processException(input: File, relativePath: String, e: Exception)

    private fun processSourceFile(input: File, relativePath: String) {
        try {
            val parser = instantiateParser(input)
            if (parser == null) {
                if (verbose) {
                    echo("skipping ${input.absolutePath}", trailingNewline = true)
                }
                return
            }
            if (verbose) {
                echo("processing ${input.absolutePath}", trailingNewline = true)
            }
            val parsingResult =
                parser.parse(input, Charset.forName(charset), considerPosition = !ignorePositions)
            if (verbose) {
                val nErrors = parsingResult.issues.count { it.severity == IssueSeverity.ERROR }
                val nWarnings = parsingResult.issues.count { it.severity == IssueSeverity.WARNING }
                if (nErrors == 0 && nWarnings == 0) {
                    echo("  no errors and no warnings", trailingNewline = true)
                } else {
                    if (nErrors == 0) {
                        echo("  $nWarnings warnings", trailingNewline = true)
                    } else if (nWarnings == 0) {
                        echo("  $nErrors errors", trailingNewline = true)
                    } else {
                        echo("  $nErrors errors and $nWarnings warnings", trailingNewline = true)
                    }
                }
            }
            processResult(input, relativePath, parsingResult, parser)
        } catch (e: Exception) {
            processException(input, relativePath, e)
        }
    }

    private fun processInput(input: File, explicit: Boolean = false, relativePath: String) {
        if (input.isDirectory) {
            input.listFiles()?.forEach {
                processInput(it, relativePath = relativePath + File.separator + it.name)
            }
        } else if (input.isFile) {
            processSourceFile(input, relativePath)
        } else {
            if (explicit) {
                echo(
                    "The provided input is neither a file or a directory, we will ignore it: " +
                        "${input.absolutePath}",
                    trailingNewline = true
                )
            } else {
                // ignore silently
            }
        }
    }
}

fun File.changeExtension(newExtension: String): File {
    var name = this.name
    val i = name.lastIndexOf('.')
    return if (i == -1) {
        val prefix = this.path.substring(0, this.path.length - this.name.length)
        name = "$name.$newExtension"
        File("${prefix}$name")
    } else {
        val prefix = this.path.substring(0, this.path.length - this.name.length)
        name = name.substring(0, i + 1) + newExtension
        File("${prefix}$name")
    }
}
