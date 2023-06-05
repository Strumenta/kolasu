@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import com.strumenta.kolasu.model.observable.ReferenceToNodeObserver
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName

object AutoObserveReferenceOrigin : IrDeclarationOriginImpl("AutoObserveReference", true)

/**
 * Make a certain field observable.
 */
class FieldObservableExtension(val pluginContext: IrPluginContext) : IrElementTransformerVoidWithContext() {

    val notifyOfPropertyChange: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(
        FqName("${Node::class.qualifiedName}.notifyOfPropertyChange")
    ).single()

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.declareReference()) {
            //     init {
            //        ref.subscribe(ReferenceToNodeObserver(this, "ref"))
            //    }

            val registerObserver = pluginContext.referenceFunctions(
                FqName("${ReferenceByName::class.qualifiedName}.subscribe")
            ).single()

            val propertyGetter = declaration.getter!!
            val irClass = declaration.parentAsClass

            val anonymousInitializerSymbolImpl = IrFactoryImpl.createAnonymousInitializer(
                declaration.startOffset,
                declaration.endOffset,
                IrDeclarationOrigin.GeneratedByPlugin(object : GeneratedDeclarationKey() {}),
                IrAnonymousInitializerSymbolImpl(),
                false
            )
            anonymousInitializerSymbolImpl.body =
                DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(-1, -1)
                ) {
                    +irCall(registerObserver).apply {
                        val thisValue = irClass.thisReceiver!!
                        val referenceToNodeObserverConstructor = pluginContext.referenceConstructors(
                            FqName(ReferenceToNodeObserver::class.qualifiedName!!)
                        ).single()
                        // dispatchReceiver: p5 -> this.getP5()
                        dispatchReceiver = irCall(propertyGetter).apply {
                            dispatchReceiver = irGet(thisValue)
                        }
                        // ReferenceToNodeObserver(this, "ref")
                        putValueArgument(
                            0,
                            irCallConstructor(referenceToNodeObserverConstructor, emptyList()).apply {
                                putValueArgument(0, irGet(thisValue))
                                putValueArgument(
                                    1,
                                    declaration.name.identifier.toIrConst(pluginContext.irBuiltIns.stringType)
                                )
                            }
                        )
                    }
                }
            anonymousInitializerSymbolImpl.parent = irClass
            irClass.declarations.add(anonymousInitializerSymbolImpl)

//            val prevBody = declaration.setter?.body
//            if (prevBody == null) {
//                if (declaration.setter == null) {
//                    declaration.setter = IrFactoryImpl.createFunction(declaration.startOffset, declaration.endOffset, AutoObserveReferenceOrigin,
//                        IrSimpleFunctionSymbolImpl(),
//                        Name.identifier("<set-${declaration.name}>"),
//                        DescriptorVisibilities.PUBLIC,
//                        Modality.FINAL,
//                        pluginContext.irBuiltIns.unitType,
//                        false,
//                        false,
//                        false,
//                        false,
//                        false,
//                        false,
//                        false,
//                        true
//                    )
//                    val anonymousInitializerSymbolImpl = IrFactoryImpl.createAnonymousInitializer(
//                        -1,
//                        -1,
//                        IrDeclarationOrigin.GeneratedByPlugin(object : GeneratedDeclarationKey() {}),
//                        IrAnonymousInitializerSymbolImpl(),
//                        false
//                    )
//                    declaration.setter!!.body = DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
//                        IrFactoryImpl.createBlockBody(-1, -1)
//                    ) {
//
//                    }
//                } else {
//                    TODO()
//                }
//            } else {
//                TODO()
//            }
        } else {
            val irContext = pluginContext
            val prevBody = declaration.setter?.body
            if (prevBody != null && declaration.setter!!.origin == IrDeclarationOrigin.DEFAULT_PROPERTY_ACCESSOR) {
                declaration.setter!!.body =
                    DeclarationIrBuilder(irContext, declaration.setter!!.symbol).irBlockBody(declaration.setter!!) {
                        // notifyOfPropertyChange("<name of property>", field, value)
                        +irCall(
                            notifyOfPropertyChange,
                            pluginContext.irBuiltIns.unitType,
                            valueArgumentsCount = 3,
                            typeArgumentsCount = 0,
                            origin = IrStatementOrigin.INVOKE
                        ).apply {
                            val thisParameter: IrValueParameter = declaration.setter!!.allParameters[0]
                            val valueParameter: IrValueParameter = declaration.setter!!.allParameters[1]

                            this.dispatchReceiver = irGet(thisParameter)

                            // "<name of property>"
                            putValueArgument(0, declaration.name.identifier.toIrConst(irContext.irBuiltIns.stringType))
                            // current backing field value
                            putValueArgument(1, irGetField(irGet(thisParameter), declaration.backingField!!))
                            // value passed to the setter
                            putValueArgument(2, irGet(valueParameter))
                        }

                        // We put back the assignment, which should be the only statement originally present
                        for (statement in prevBody.statements) +statement
                    }
            }
        }
        return declaration
    }
}
