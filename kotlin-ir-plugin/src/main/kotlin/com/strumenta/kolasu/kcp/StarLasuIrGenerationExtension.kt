@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.ReferenceByName
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.wasm.ir2wasm.allSuperInterfaces
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.realOverrideTarget
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallOp
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irLetS
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.linkage.partial.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertySymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.addArguments
import org.jetbrains.kotlin.ir.util.addFakeOverrides
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.isOverridableOrOverrides
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.ir.util.overrides
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getClassFqNameUnsafe

class StarLasuIrGenerationExtension(
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    private fun checkASTNode(
        irClass: IrClass,
        pluginContext: IrPluginContext,
        isBaseNode: Boolean,
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
            irClass.accept(FieldObservableExtension(pluginContext, isBaseNode), null)
            irClass.accept(SettingParentExtension(pluginContext, messageCollector, isBaseNode), null)
            if (isBaseNode) {
                if (irClass.modality != Modality.SEALED && irClass.modality != Modality.ABSTRACT) {
                    overrideCalculateFeaturesBody(irClass, pluginContext)
                    overrideCalculateNodeTypeBody(irClass, pluginContext)
                }
            }
    }

    private fun overrideCalculateFeaturesBody(irClass: IrClass, pluginContext: IrPluginContext) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateFeaturesBody for ${irClass.name.identifier}",
            irClass.compilerSourceLocation
        )
        val function = irClass.functions.find { it.name.identifier == "calculateFeatures" }
        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function calculateFeatures FOUND",
                irClass.compilerSourceLocation
            )
            val mutableListOf =
                pluginContext
                    .referenceFunctions(CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableListOf"))).find { it.owner!!.valueParameters.size == 0 }!!
            function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
            ) {
                // We create a mutable list with no parameters
                //val mutableListOfCall = irCall(mutableListOf)
                // Then we call apply on it
                //apply {  }

                val variable = irTemporary(irCall(mutableListOf))
                +variable
                val mutableListClass = pluginContext.referenceClass(MutableList::class.classId) ?: throw RuntimeException("MutableList not found")
                val add = mutableListClass.functions.find { it.owner.name.identifier == "add" } ?: throw RuntimeException("MutableList.add not found")
                irClass.properties.forEach { property ->
                    +irCall(add).apply {
                        dispatchReceiver = irGet(variable)

//                        class FeatureDescription(
//                            val name: String,
//                            val provideNodes: Boolean,
//                            val multiplicity: Multiplicity,
//                            val valueProvider: () -> Any?,
//                            val featureType: FeatureType,
//                            val derived: Boolean = false,

                            putValueArgument(0, irNull())
                    }
                }

                +irReturn(irGet(variable))
            }
        }
    }

    private fun overrideCalculateNodeTypeBody(irClass: IrClass, pluginContext: IrPluginContext) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateNodeTypeBody for ${irClass.name.identifier}",
            irClass.compilerSourceLocation
        )
        val function = irClass.functions.find { it.name.identifier == "calculateNodeType" }
        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function calculateNodeType FOUND",
                irClass.compilerSourceLocation
            )
            function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
            ) {
                +irReturn(irString(irClass.name.identifier))
            }
        }
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "COMPILER PLUGIN IS GENERATING",
        )
        moduleFragment.files.forEach { irFile ->
            irFile.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                val isASTNode =
                    irClass.getAllSuperclasses().any {
                        it.kotlinFqName.toString() == Node::class.qualifiedName
                    }
                val isBaseNode =
                    irClass.getAllSuperclasses().any {
                        it.kotlinFqName.toString() == BaseNode::class.qualifiedName
                    }
                if (isASTNode || isBaseNode) {
                    if (isASTNode) {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "AST class ${irClass.kotlinFqName} identified",
                            irClass.compilerSourceLocation,
                        )
                    } else {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "BaseNode subclass ${irClass.kotlinFqName} identified",
                            irClass.compilerSourceLocation,
                        )
                    }
                    checkASTNode(irClass, pluginContext, isBaseNode)
                }
            }
        }
    }
}

