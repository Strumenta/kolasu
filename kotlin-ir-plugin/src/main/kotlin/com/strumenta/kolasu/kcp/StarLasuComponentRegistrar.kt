@file:OptIn(ExperimentalCompilerApi::class)

package com.strumenta.kolasu.kcp

import com.google.auto.service.AutoService
import com.strumenta.kolasu.kcp.fir.StarLasuFirExtensionsRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.jvm.extensions.ClassGeneratorExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@AutoService(CompilerPluginRegistrar::class)
class StarLasuComponentRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean
        get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)

        FirExtensionRegistrarAdapter.registerExtension(StarLasuFirExtensionsRegistrar(messageCollector))
        IrGenerationExtension.registerExtension(StarLasuIrGenerationExtension(messageCollector))
        IrGenerationExtension.registerExtension(LanguageIrGenerationExtension(messageCollector))
    }
}
