package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension

private fun gen(firSession: FirSession) {

}

class StarLasuFirExtensionsRegistrar(private val messageCollector: MessageCollector) : FirExtensionRegistrar() {



    override fun ExtensionRegistrarContext.configurePlugin() {
        //+::ConceptGenerator
        +::BaseNodeGenerator
    }
}
