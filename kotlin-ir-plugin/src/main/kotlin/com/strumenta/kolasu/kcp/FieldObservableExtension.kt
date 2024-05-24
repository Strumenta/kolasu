@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.ReferenceValue
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
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Make a certain field observable.
 */
class FieldObservableExtension(
    val pluginContext: IrPluginContext,
    isMPNode: Boolean,
) : IrElementTransformerVoidWithContext() {
    val notifyOfPropertyChange: IrSimpleFunctionSymbol by lazy {
        val callableId =
            if (isMPNode) {
                CallableId(
                    ClassId.topLevel(FqName(MPNode::class.qualifiedName!!)),
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

            val referenceValueSetContainerMethod =
                pluginContext
                    .referenceFunctions(
                        ReferenceValue::class,
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
                    +irCall(referenceValueSetContainerMethod).apply {
                        val thisValue = irClass.thisReceiver!!
                        // dispatchReceiver: p5 -> this.getP5()
                        dispatchReceiver =
                            irCall(propertyGetter).apply {
                                dispatchReceiver = irGet(thisValue)
                            }
                        // passing "this"
                        putValueArgument(0, irGet(thisValue))
                        // passing the reference
                        val nodeSubClass = declaration.parentAsClass
                        putValueArgument(
                            1,
                            referenceByName(
                                pluginContext, irGet(thisValue),
                                declaration.name.identifier,
                            ),
                        )
                    }
                }
            anonymousInitializerSymbolImpl.parent = irClass
            irClass.declarations.add(anonymousInitializerSymbolImpl)
        } else if (declaration.declareContainment()) {
        } else if (declaration.declareAttribute()) {
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

                            // attribute: myClass.concept.property(attributeName)
                            val nodeSubClass = declaration.parentAsClass
                            putValueArgument(
                                0,
                                propertyByName(
                                    pluginContext, nodeSubClass,
                                    declaration.name.identifier,
                                ),
                            )
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
