package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.parameters.options.*
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.Result
import java.io.File

class StatsCollector {
    private var filesProcessed: Int = 0
    private var filesWithExceptions: Int = 0
    private val filesProcessedSuccessfully: Int
        get() = filesProcessed - filesWithExceptions
    private var filesWithErrors: Int = 0
    private val filesWithoutErrors: Int
        get() = filesProcessedSuccessfully - filesWithErrors
    private var totalErrors: Int = 0

    fun registerException(input: File, e: Exception) {
        filesProcessed += 1
        filesWithExceptions += 1
    }

    fun registerResult(input: File, result: Result<out Node>) {
        filesProcessed += 1
        val errors = result.issues.filter { it.severity == IssueSeverity.ERROR }.count()
        if (errors > 0) {
            filesWithErrors += 1
            totalErrors += errors
        }
    }

    fun print(println: (s: String)->Unit = ::println) {
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
}

class ErrorStatsCollector {
    private val errorsPrevalence = HashMap<String, Int>()

    fun print(println: (s: String)->Unit = ::println) {
        if (errorsPrevalence.isEmpty()) {
            return
        }
        println("# Errors prevalence")
        errorsPrevalence.entries.sortedByDescending { it.value }.forEach { entry ->
            println(" ${entry.key.padEnd(50)} : ${entry.value} occurrences")
        }
        println("")
    }

    fun registerResult(result: Result<out Node>) {
        val errors = result.issues.filter { it.severity == IssueSeverity.ERROR }
        errors.forEach { error ->
            val message = canonizeMessage(error.message)
            errorsPrevalence[message] = (errorsPrevalence[message] ?: 0) + 1
        }
    }

    private fun canonizeMessage(message: String): String {
        return message.replace("[0-9]", "*")
    }
}

class StatsCommand<R : Node, P : ASTParser<R>>(parserInstantiator: ParserInstantiator<P>) :
    ASTProcessingCommand<R, P>(
        parserInstantiator,
        help = "Produced various stats on parsing",
        name = "stats"
    ) {

    private val printStats by option("--stats", "-s")
        .help("Print statistics on the number of files parsed correctly")
        .flag(default = true)
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

    private var statsCollector: StatsCollector? = null
    private var errorStatsCollector: ErrorStatsCollector? = null

    override fun initializeRun() {
        if (printStats) {
            statsCollector = StatsCollector()
        }
        if (printErrorStats) {
            errorStatsCollector = ErrorStatsCollector()
        }
    }

    override fun finalizeRun() {
        statsCollector?.print {text: String -> echo(text, trailingNewline = true)}
        errorStatsCollector?.print {text: String -> echo(text, trailingNewline = true)}
    }

    override fun processException(input: File, relativePath: String, e: Exception) {
        statsCollector?.registerException(input, e)
    }

    override fun processResult(input: File, relativePath: String, result: Result<R>) {
        statsCollector?.registerResult(input, result)
        errorStatsCollector?.registerResult(result)
    }
}
