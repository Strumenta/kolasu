package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.traversing.walkDescendants
import com.strumenta.kolasu.validation.IssueSeverity
import java.io.File

interface StatsCollector {
    fun registerException(input: File, e: Exception) {
    }

    fun registerResult(input: File, result: ParsingResult<out Node>)

    fun print(println: (s: String) -> Unit = ::println)

    fun saveToCSV()

    val description: String

    val csvFile: File
}

class GlobalStatsCollector : StatsCollector {
    private var filesProcessed: Int = 0
    private var filesWithExceptions: Int = 0
    private val filesProcessedSuccessfully: Int
        get() = filesProcessed - filesWithExceptions
    private var filesWithErrors: Int = 0
    private val filesWithoutErrors: Int
        get() = filesProcessedSuccessfully - filesWithErrors
    private var totalErrors: Int = 0

    override fun registerException(input: File, e: Exception) {
        filesProcessed += 1
        filesWithExceptions += 1
    }

    override fun registerResult(input: File, result: ParsingResult<out Node>) {
        filesProcessed += 1
        val errors = result.issues.filter { it.severity == IssueSeverity.ERROR }.count()
        if (errors > 0) {
            filesWithErrors += 1
            totalErrors += errors
        }
    }

    override fun print(println: (s: String) -> Unit) {
        println("== Stats ==")
        println("")
        println(" [Did processing complete?]")
        println("  files processed         : $filesProcessed")
        println("     processing failed    : $filesWithExceptions")
        println("     processing completed : $filesProcessedSuccessfully")
        println("")
        println(" [Did processing complete successfully?]")
        println("  processing completed with errors    : $filesWithErrors")
        println("  processing completed without errors : $filesWithoutErrors")
        println("  total number of errors              : $totalErrors")
    }

    override fun saveToCSV() {
        csvWriter().open(csvFile.absolutePath) {
            writeRow(listOf("Key", "Value"))
            writeRow(listOf("filesProcessed", filesProcessed))
            writeRow(listOf("filesWithExceptions", filesWithExceptions))
            writeRow(listOf("filesProcessedSuccessfully", filesProcessedSuccessfully))
            writeRow(listOf("filesWithErrors", filesWithErrors))
            writeRow(listOf("filesWithoutErrors", filesWithoutErrors))
            writeRow(listOf("totalErrors", totalErrors))
        }
    }

    override val description: String
        get() = "Global Stats"
    override val csvFile: File
        get() = File("global-stats.csv")
}

class NodeStatsCollector(val simpleNames: Boolean) : StatsCollector {
    private val nodePrevalence = HashMap<String, Int>()

    override fun registerException(input: File, e: Exception) {
    }

    override fun registerResult(input: File, result: ParsingResult<out Node>) {
        result.root?.apply {
            nodePrevalence[this.nodeType] = nodePrevalence.getOrDefault(this.nodeType, 0) + 1
            walkDescendants().forEach {
                nodePrevalence[it.nodeType] = nodePrevalence.getOrDefault(it.nodeType, 0) + 1
            }
        }
    }

    override fun print(println: (s: String) -> Unit) {
        val length = if (simpleNames) 25 else 50
        println("== Node Stats ==")
        println("")
        nodePrevalence.keys.sorted().forEach { nodeType ->
            var label = nodeType
            if (simpleNames) {
                label = label.split(".").last()
            }
            println("  ${label.padEnd(length)}: ${nodePrevalence[nodeType]}")
        }
        println("")
        println("  ${"total number of nodes".padEnd(length)}: ${nodePrevalence.values.sum()}")
    }

    override fun saveToCSV() {
        csvWriter().open(csvFile.absolutePath) {
            writeRow(listOf("Key", "Value"))
            nodePrevalence.keys.sorted().forEach { nodeType ->
                var label = nodeType
                if (simpleNames) {
                    label = label.split(".").last()
                }
                writeRow(listOf(label, nodePrevalence[nodeType]))
            }
            writeRow(listOf("total", nodePrevalence.values.sum()))
        }
    }

    override val description: String
        get() = "Node Stats"
    override val csvFile: File
        get() = File("node-stats.csv")
}

class ErrorStatsCollector : StatsCollector {
    private val errorsPrevalence = HashMap<String, Int>()

    override fun print(println: (s: String) -> Unit) {
        if (errorsPrevalence.isEmpty()) {
            return
        }
        println("# Errors prevalence")
        errorsPrevalence.entries.sortedByDescending { it.value }.forEach { entry ->
            println(" ${entry.key.padEnd(50)} : ${entry.value} occurrences")
        }
        println("")
    }

    override fun registerResult(input: File, result: ParsingResult<out Node>) {
        val errors = result.issues.filter { it.severity == IssueSeverity.ERROR }
        errors.forEach { error ->
            val message = canonizeMessage(error.message)
            errorsPrevalence[message] = (errorsPrevalence[message] ?: 0) + 1
        }
    }

    private fun canonizeMessage(message: String): String {
        return message.replace("[0-9]", "*")
    }

    override fun saveToCSV() {
        println("# Errors prevalence")
        errorsPrevalence.entries.sortedByDescending { it.value }.forEach { entry ->
            println(" ${entry.key.padEnd(50)} : ${entry.value} occurrences")
        }
        println("")
        csvWriter().open(csvFile.absolutePath) {
            writeRow(listOf("Key", "Value"))
            errorsPrevalence.entries.sortedByDescending { it.value }.forEach { entry ->
                writeRow(listOf(entry.key, entry.value))
            }
        }
    }

    override val description: String
        get() = "Error Stats"
    override val csvFile: File
        get() = File("error-stats.csv")
}

/**
 * Command to calcualte statistics on the ASTs produced and print them.
 */
class StatsCommand<R : Node, P : ASTParser<R>>(parserInstantiator: ParserInstantiator<P>) :
    ASTProcessingCommand<R, P>(
        parserInstantiator,
        help = "Produced various stats on parsing.",
        name = "stats"
    ) {

    private val printStats by option("--stats", "-s")
        .help("Print statistics on the number of files parsed correctly")
        .flag("--no-stats", "-ns", default = true)
    private val printErrorStats by option("--error-stats", "-e")
        .help("Print statistics on the prevalence of the different error messages")
        .flag(default = false)
    private val nodeStats by option("--node-stats", "-n")
        .help("Print statistics on the prevalence of the different kinds of nodes")
        .flag(default = false)
    private val nodeTypesToExclude by option("--exclude", "-x")
        .help("Remove the node types indicated")
        .split(",")
        .default(emptyList())
    private val simpleNames by option("--simple-names", "-sn")
        .help("Print simple names instead of qualified names")
        .flag(default = false)
    private val csv by option("--csv")
        .help("Save stats in CSV files instead of printing them")
        .flag(default = false)

    private val collectors = mutableListOf<StatsCollector>()

    override fun initializeRun() {
        if (printStats) {
            collectors.add(GlobalStatsCollector())
        }
        if (printErrorStats) {
            collectors.add(ErrorStatsCollector())
        }
        if (nodeStats) {
            collectors.add(NodeStatsCollector(simpleNames))
        }
    }

    override fun finalizeRun() {
        collectors.forEach {
            if (csv) {
                echo("Saving ${it.description} to ${it.csvFile}", trailingNewline = true)
                it.saveToCSV()
            } else {
                it.print { text: String -> echo(text, trailingNewline = true) }
            }
        }
    }

    override fun processException(input: File, relativePath: String, e: Exception) {
        collectors.forEach {
            it.registerException(input, e)
        }
    }

    override fun processResult(input: File, relativePath: String, result: ParsingResult<R>, parser: P) {
        collectors.forEach {
            it.registerResult(input, result)
        }
    }
}
