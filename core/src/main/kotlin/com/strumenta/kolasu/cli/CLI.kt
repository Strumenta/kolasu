package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.parsing.KolasuParser
import com.strumenta.kolasu.parsing.ParsingResult
import org.antlr.v4.runtime.tree.ParseTree
import java.io.File
import kotlin.system.exitProcess

data class CLIContext<R : Node>(val language: KolasuParser<R, *, *>, var input: String? = null)

class ParserCLI<R : Node>(val parser: KolasuParser<R, *, *>) : CliktCommand() {
    val input by argument()
    val context by findOrSetObject { CLIContext(parser) }
    override fun run() {
        context.input = input
    }
}

open class ParsingCommand<R : Node>(
    help: String = "",
    epilog: String = "",
    name: String? = null,
    invokeWithoutSubcommand: Boolean = false,
    printHelpOnEmptyArgs: Boolean = false,
    helpTags: Map<String, String> = emptyMap(),
    autoCompleteEnvvar: String? = "",
    allowMultipleSubcommands: Boolean = false,
    treatUnknownOptionsAsArgs: Boolean = false,
    val exitOnFail: Boolean = true,
    val printASTSummary: Boolean = true
) : CliktCommand(
    help, epilog, name, invokeWithoutSubcommand, printHelpOnEmptyArgs, helpTags, autoCompleteEnvvar,
    allowMultipleSubcommands, treatUnknownOptionsAsArgs
) {
    var result: ParsingResult<R>? = null
    val context by requireObject<CLIContext<R>>()
    override fun run() {
        val input = File(context.input!!)
        print("Parsing $input... ")
        result = context.language.parse(input)
        if (result!!.correct) {
            println("OK.")
        } else {
            println("failed. Issues: ")
            result!!.issues.forEach {
                println('\t' + it.message + if (it.position != null) " @ ${it.position}" else "")
            }
            if (exitOnFail) {
                exitProcess(2)
            }
        }
        done()
    }

    open fun done() {
        println(
            "Done (${result!!.time}ms of which ${result!!.time!! - result!!.firstStage!!.time!!}ms to build the AST)."
        )
        val node = result!!.root!!
        val parseTree = node.parseTreeNode!!
        val lines = parseTree.stop.line - parseTree.start.line
        println(
            "The file has $lines lines and ${result!!.code?.length ?: "unknown"} characters. " +
                "The parse tree has ${countParseTreeNodes(parseTree)} nodes " +
                "(${parseTree.start.inputStream.size()} tokens)."
        )
        if (printASTSummary) {
            println("The AST has ${countASTNodes(node)} nodes.")
        }
    }

    fun countASTNodes(node: Node): Int = 1 + (node.children.map { countASTNodes(it) }.reduceOrNull(Int::plus) ?: 0)

    fun countParseTreeNodes(parseTree: ParseTree): Int {
        var result = 1
        for (i in 0 until parseTree.childCount) {
            result += countParseTreeNodes(parseTree.getChild(i))
        }
        return result
    }
}
