package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktConsole
import com.strumenta.kolasu.cli.ParserInstantiator
import com.strumenta.kolasu.emf.EMFMetamodelSupport
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.parsing.ASTParser
import org.eclipse.emf.ecore.EPackage
import java.util.function.Supplier

class EMFCLITool<R : Node, P>(
    parserInstantiator: ParserInstantiator<P>,
    metamodelGenerator: Supplier<EPackage>,
    replacedConsole: CliktConsole? = null
) : CliktCommand(invokeWithoutSubcommand = false) where P : EMFMetamodelSupport, P : ASTParser<R> {
    init {
        subcommands(EMFModelCommand(parserInstantiator), EMFMetaModelCommand(metamodelGenerator))
        context { replacedConsole?.apply { console = replacedConsole } }
    }

    override fun run() {
        // Nothing to do here
    }
}
