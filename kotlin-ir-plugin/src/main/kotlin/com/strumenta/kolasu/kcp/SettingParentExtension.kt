@file:OptIn(ObsoleteDescriptorBasedAPI::class, FirIncompatiblePluginAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.observable.MultiplePropertyListObserver
import com.strumenta.kolasu.model.observable.ObservableList
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNotEquals
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFakeOverrideSymbolBase
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull

object StarLasuGeneratedDeclarationKey : GeneratedDeclarationKey()

/**
 * Set the parent appropriately when modifying a containment value.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
class SettingParentExtension(
    val pluginContext: IrPluginContext,
    val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        val prevBody = declaration.setter?.body
        if (prevBody != null && declaration.setter!!.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
            if (declaration.declareSingleOrOptionalContainment()) {
                val thisParameter: IrValueParameter = declaration.setter!!.allParameters[0]
                val valueParameter: IrValueParameter = declaration.setter!!.allParameters[1]
                declaration.setter!!.body =
                    DeclarationIrBuilder(pluginContext, declaration.setter!!.symbol)
                        .irBlockBody(declaration.setter!!) {
                            val propertyTypeClassSymbol: IrClassSymbol =
                                (declaration.backingField!!.type as IrSimpleType)
                                    .classifier as IrClassSymbol

                            for (statement in prevBody.statements) +statement

                            val parentPropertySymbol =
                                pluginContext
                                    .referenceProperties(
                                        CallableId(
                                            ClassId.topLevel(propertyTypeClassSymbol.descriptor.fqNameOrNull()!!),
                                            Name
                                                .identifier("parent"),
                                        ),
                                    ).single()
                            val parentSetter = if (parentPropertySymbol is IrFakeOverrideSymbolBase<*, *, *>)
                                (parentPropertySymbol.originalSymbol as IrPropertySymbol).owner.setter
                                //throw IllegalStateException("We cannot get the owner of $parentPropertySymbol in ${propertyTypeClassSymbol.descriptor.fqNameOrNull()}")
                            else {
                                parentPropertySymbol.owner.setter!!
                            } as IrFunction
                            // TODO remove from previous parent: field?.removeChild(this)
                            // value?.parent = this -> if (value != null) {value.parent = this}
                            +irIfThen(
                                irNotEquals(irGet(valueParameter), irNull()),
                                irCall(parentSetter).apply {
                                    this.dispatchReceiver = irGet(valueParameter)
                                    this.putValueArgument(0, irGet(thisParameter))
                                },
                            )
                        }
            } else if (declaration.declareMultipleContainment()) {
                if (declaration.backingField!!.type.isAssignableTo(ObservableList::class)) {
                    val irClass = declaration.parentAsClass

                    //    init {
                    //        p5.registerObserver(MultiplePropertyListObserver(this, "p5"))
                    //    }
                    val registerObserver =
                        pluginContext
                            .referenceFunctions(
                                CallableId(
                                    ClassId.topLevel(FqName(ObservableList::class.qualifiedName!!)),
                                    Name.identifier("subscribe"),
                                ),
                            ).single()

                    val propertyGetter = declaration.getter!!

                    val anonymousInitializerSymbolImpl =
                        IrFactoryImpl.createAnonymousInitializer(
                            -1,
                            -1,
                            IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey),
                            IrAnonymousInitializerSymbolImpl(),
                            false,
                        )
                    anonymousInitializerSymbolImpl.body =
                        DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
                            IrFactoryImpl.createBlockBody(-1, -1),
                        ) {
                            +irCall(registerObserver).apply {
                                val thisValue = irClass.thisReceiver!!
                                val multiplePropertyListObserverConstructor =
                                    pluginContext
                                        .referenceConstructors(MultiplePropertyListObserver::class)
                                        .find {
                                            it
                                                .owner
                                                .valueParameters[1]
                                                .name
                                                .identifier == "containmentName"
                                        }!!
                                // dispatchReceiver: p5 -> this.getP5()
                                dispatchReceiver =
                                    irCall(propertyGetter).apply {
                                        dispatchReceiver = irGet(thisValue)
                                    }
                                // MultiplePropertyListObserver(this, concept.requireContainment("p5"))
                                putValueArgument(
                                    0,
                                    irCallConstructor(multiplePropertyListObserverConstructor, emptyList()).apply {
                                        putValueArgument(0, irGet(thisValue))
                                        putValueArgument(
                                            1,
                                            declaration.name.identifier.toIrConst(pluginContext.irBuiltIns.stringType),
                                        )
                                    },
                                )
                            }
                        }
                    anonymousInitializerSymbolImpl.parent = irClass
                    irClass.declarations.add(anonymousInitializerSymbolImpl)
                } else {
                    messageCollector.report(
                        CompilerMessageSeverity.ERROR,
                        "AST Nodes should use ObservableLists (see ${declaration.fqNameWhenAvailable!!})",
                    )
                }
            }
        }

        return declaration
    }
}
