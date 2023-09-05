package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalInterface
import com.strumenta.kolasu.cli.ParserInstantiator
import com.strumenta.kolasu.emf.EMFMetamodelSupport
import com.strumenta.kolasu.emf.EcoreEnabledParser
import com.strumenta.kolasu.model.Node

class EMFCLITool<R : Node, P>(
    parserInstantiator: ParserInstantiator<P>,
    metamodelSupport: EMFMetamodelSupport,
    replacedConsole: TerminalInterface? = null
) : CliktCommand(invokeWithoutSubcommand = false) where P : EcoreEnabledParser<R, *, *, *> {
    init {
        subcommands(EMFModelCommand(parserInstantiator), EMFMetaModelCommand(metamodelSupport))
        context { replacedConsole?.apply { terminal = Terminal(replacedConsole) } }
    }

    override fun run() {
        // Nothing to do here
    }
}
