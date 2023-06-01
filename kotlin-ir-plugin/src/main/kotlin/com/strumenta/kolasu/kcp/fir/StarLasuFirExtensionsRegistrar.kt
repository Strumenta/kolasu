package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

class StarLasuFirExtensionsRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::ConceptGenerator
    }
}
