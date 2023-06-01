@file:OptIn(FirIncompatiblePluginAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.Node
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.util.allParameters
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.name.FqName

/**
 * Make a certain field observable.
 */
class FieldObservableExtension(val pluginContext: IrPluginContext) : IrElementTransformerVoidWithContext() {

    override fun visitPropertyNew(declaration: IrProperty): IrStatement {
        val notifyOfPropertyChange: IrSimpleFunctionSymbol = pluginContext.referenceFunctions(
            FqName("${Node::class.qualifiedName}.notifyOfPropertyChange")
        ).single()

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
        return declaration
    }
}
