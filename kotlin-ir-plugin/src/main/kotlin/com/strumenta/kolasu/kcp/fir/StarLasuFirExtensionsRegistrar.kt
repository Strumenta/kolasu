package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class StarLasuFirExtensionsRegistrar(
    private val messageCollector: MessageCollector,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        // +::ConceptGenerator
        +::MPNodeGenerator
        +::ErrorClassGenerator
        +::LanguageGenerator
    }
}
