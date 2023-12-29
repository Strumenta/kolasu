package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser

/**
 * This class is intended to be instantiated by providing an instantiation that is aware of a specific parser.
 *
 * For example, it could be used in this way:
 *
 * ```
 * fun main(args: Array<String>) = CLITool(ParserInstantiator { MyParser() }).main(args)
 * ```
 *
 * In the future we may want to provide the name of the actual parser as a class name and instantiate it through
 * reflection.
 *
 * Alternatively, the user may want to group several commands, including the ones used here and others coming
 * from extensions (such as the EMF one) or created within specific projects.
 *
 * For example:
 * ```
 * class MyCLI<R : Node, P : ASTParser<R>>(
 *   parserInstantiator: ParserInstantiator<P>,
 *   ) : CliktCommand(invokeWithoutSubcommand = false) {
 *   init {
 *      subcommands(ASTSaverCommand(parserInstantiator), StatsCommand(parserInstantiator), MyOtherCommand(...))
 *   }
 *
 *   override fun run() = Unit
 *   }
 * ```
 */
class CLITool<R : Node, P : ASTParser<R>>(
    parserInstantiator: ParserInstantiator<P>,
    replacedConsole: CliktConsole? = null,
) : CliktCommand(invokeWithoutSubcommand = false) {
    init {
        subcommands(ASTSaverCommand(parserInstantiator), StatsCommand(parserInstantiator))
        context { replacedConsole?.apply { console = replacedConsole } }
    }

    override fun run() = Unit
}
