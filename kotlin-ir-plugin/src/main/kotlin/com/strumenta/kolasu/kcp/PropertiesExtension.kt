@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI

// object AutoObserveReferenceOrigin : IrDeclarationOriginImpl("AutoObserveReference", true)

/**
 * Make a certain field observable.
 */
// class PropertiesExtension(
//    val pluginContext: IrPluginContext,
//    isBaseNode: Boolean,
// ) : IrElementTransformerVoidWithContext() {
// //    val notifyOfPropertyChange: IrSimpleFunctionSymbol by lazy {
// //        val callableId = if (isBaseNode) {
// //            CallableId(
// //                ClassId.topLevel(FqName(BaseNode::class.qualifiedName!!)),
// //                Name.identifier("notifyOfPropertyChange")
// //            )
// //        } else {
// //            CallableId(
// //                ClassId.topLevel(FqName(Node::class.qualifiedName!!)),
// //                Name.identifier("notifyOfPropertyChange")
// //            )
// //        }
// //        pluginContext
// //            .referenceFunctions(
// //                callableId,
// //            ).single()
// //    }
//
//
// }
