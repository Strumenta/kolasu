package com.strumenta.kolasu.lionweb

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated

class LionWebSymbolProcessor : SymbolProcessor {
    override fun process(resolver: Resolver): List<KSAnnotated> {
        println("executing LionWebSymbolProcessor")
        resolver.getAllFiles().forEach {
            println("PROCESSING FILE ${it.fileName}")
        }
        return emptyList()
    }

}

class LanguageExporter : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        println("CREATING LionWebSymbolProcessor")
        return LionWebSymbolProcessor()
    }

}