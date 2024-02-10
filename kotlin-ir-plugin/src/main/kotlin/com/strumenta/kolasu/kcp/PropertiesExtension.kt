@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
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

// object AutoObserveReferenceOrigin : IrDeclarationOriginImpl("AutoObserveReference", true)

/**
 * Make a certain field observable.
 */
//class PropertiesExtension(
//    val pluginContext: IrPluginContext,
//    isBaseNode: Boolean,
//) : IrElementTransformerVoidWithContext() {
////    val notifyOfPropertyChange: IrSimpleFunctionSymbol by lazy {
////        val callableId = if (isBaseNode) {
////            CallableId(
////                ClassId.topLevel(FqName(BaseNode::class.qualifiedName!!)),
////                Name.identifier("notifyOfPropertyChange")
////            )
////        } else {
////            CallableId(
////                ClassId.topLevel(FqName(Node::class.qualifiedName!!)),
////                Name.identifier("notifyOfPropertyChange")
////            )
////        }
////        pluginContext
////            .referenceFunctions(
////                callableId,
////            ).single()
////    }
//
//
//}
