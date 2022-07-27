package com.strumenta.kolasu.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser

class CLITool<R : Node, P : ASTParser<R>>(
    parserInstantiator: ParserInstantiator<P>,
    replacedConsole: CliktConsole? = null
) : CliktCommand(invokeWithoutSubcommand = false) {
    init {
        subcommands(ASTSaverCommand(parserInstantiator), StatsCommand(parserInstantiator))
        context { replacedConsole?.apply { console = replacedConsole } }
    }

    override fun run() {
        // Nothing to do here
    }
}
