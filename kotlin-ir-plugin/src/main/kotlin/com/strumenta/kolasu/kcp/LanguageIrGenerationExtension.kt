package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.language.Concept
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.irCall
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.Name

class LanguageIrGenerationExtension(private val messageCollector: MessageCollector) : BaseIrGenerationExtension() {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext
    ) {
        processMPNodeSubclasses(moduleFragment, pluginContext) { irClass ->
            irClass.declarations.filterIsInstance<IrClass>().filter { it.isCompanion }.forEach { companionIrClass ->
                val anonymousInitializerSymbolImpl =
                    IrFactoryImpl.createAnonymousInitializer(
                        irClass.startOffset,
                        irClass.endOffset,
                        IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey),
                        IrAnonymousInitializerSymbolImpl(),
                        false,
                    )
                anonymousInitializerSymbolImpl.body =
                    DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
                        IrFactoryImpl.createBlockBody(-1, -1),
                    ) {
                        val conceptProperty = companionIrClass.properties.find { it.name == Name.identifier("concept") }!!
                        // set concept
                        val conceptConstructor  =
                            pluginContext
                                .referenceConstructors(Concept::class)
                                .single()
                        val conceptValue: IrExpression = irCallConstructor(conceptConstructor, emptyList()).apply {
                            putValueArgument(0, irString(irClass.name.identifier))
                        }
                        val thisCompanion = irGet(companionIrClass.thisReceiver!!)
                        val stmt : IrStatement = irSetField(thisCompanion, conceptProperty.backingField!!, conceptValue)
                        +stmt
                    }
                anonymousInitializerSymbolImpl.parent = irClass
                companionIrClass.declarations.add(anonymousInitializerSymbolImpl)
            }
        }
    }

}