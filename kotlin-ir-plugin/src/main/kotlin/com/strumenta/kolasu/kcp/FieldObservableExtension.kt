@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceByName
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.util.toIrConst
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Make a certain field observable.
 */
class FieldObservableExtension(
    val pluginContext: IrPluginContext,
    isBaseNode: Boolean,
) : IrElementTransformerVoidWithContext() {
    val notifyOfPropertyChange: IrSimpleFunctionSymbol by lazy {
        val callableId =
            if (isBaseNode) {
                CallableId(
                    ClassId.topLevel(FqName(BaseNode::class.qualifiedName!!)),
                    Name.identifier("notifyOfPropertyChange"),
                )
            } else {
                CallableId(
                    ClassId.topLevel(FqName(Node::class.qualifiedName!!)),
                    Name.identifier("notifyOfPropertyChange"),
                )
            }
        pluginContext
            .referenceFunctions(
                callableId,
            ).single()
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        if (declaration.declareReference()) {
            //     init {
            //        ref.setContainer(this, "ref")
            //    }

            val referenceByNameSetContainerMethod =
                pluginContext
                    .referenceFunctions(
                        ReferenceByName::class,
                        "setContainer",
                    ).single()

            val propertyGetter = declaration.getter!!
            val irClass = declaration.parentAsClass

            val anonymousInitializerSymbolImpl =
                IrFactoryImpl.createAnonymousInitializer(
                    declaration.startOffset,
                    declaration.endOffset,
                    IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey),
                    IrAnonymousInitializerSymbolImpl(),
                    false,
                )
            anonymousInitializerSymbolImpl.body =
                DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(-1, -1),
                ) {
                    +irCall(referenceByNameSetContainerMethod).apply {
                        val thisValue = irClass.thisReceiver!!
                        // dispatchReceiver: p5 -> this.getP5()
                        dispatchReceiver =
                            irCall(propertyGetter).apply {
                                dispatchReceiver = irGet(thisValue)
                            }
                        // passing "this"
                        putValueArgument(0, irGet(thisValue))
                        // passing the name of the reference
                        putValueArgument(
                            1,
                            declaration.name.identifier.toIrConst(pluginContext.irBuiltIns.stringType),
                        )
                    }
                }
            anonymousInitializerSymbolImpl.parent = irClass
            irClass.declarations.add(anonymousInitializerSymbolImpl)
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
                            origin = IrStatementOrigin.INVOKE,
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
