package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.cli.ParserInstantiator
import com.strumenta.kolasu.emf.EMFMetamodelSupport
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser

class EMFCLITool<R : Node, P : ASTParser<R>>(
    parserInstantiator: ParserInstantiator<P>,
    metamodelSupport: EMFMetamodelSupport,
    replacedConsole: CliktConsole? = null
) : CliktCommand(invokeWithoutSubcommand = false) where P : EMFMetamodelSupport {
    init {
        subcommands(EMFModelCommand(parserInstantiator), EMFMetaModelCommand(metamodelSupport))
        context { replacedConsole?.apply { console = replacedConsole } }
    }

    override fun run() {
        // Nothing to do here
    }
}
