@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.Node
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor

class StarLasuIrGenerationExtension(
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    private fun checkASTNode(
        irClass: IrClass,
        pluginContext: IrPluginContext,
    ) {
        irClass.primaryConstructor?.valueParameters?.forEach { param ->
            if (param.isVal() && (param.isSingleContainment() || param.isSingleAttribute())) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "Value param ${irClass.kotlinFqName}.${param.name} is not assignable",
                    param.compilerSourceLocation,
                )
            }
        }
        irClass.accept(FieldObservableExtension(pluginContext), null)
        irClass.accept(SettingParentExtension(pluginContext, messageCollector), null)
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        moduleFragment.files.forEach { irFile ->
            irFile.declarations.filterIsInstance(IrClass::class.java).forEach { irClass ->
                val isASTNode =
                    irClass.getAllSuperclasses().any {
                        it.kotlinFqName.toString() == Node::class.qualifiedName
                    }
                if (isASTNode) {
                    messageCollector.report(
                        CompilerMessageSeverity.INFO,
                        "AST class ${irClass.kotlinFqName} identified",
                        irClass.compilerSourceLocation,
                    )
                    checkASTNode(irClass, pluginContext)
                }
            }
        }
    }
}
