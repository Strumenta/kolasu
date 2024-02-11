@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
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
import org.jetbrains.kotlin.ir.builders.irReturn
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
                }
            }
    }

    private fun overrideCalculateFeaturesBody(irClass: IrClass, pluginContext: IrPluginContext) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateFeatures for ${irClass.name.identifier}",
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
                    .referenceFunctions(CallableId(FqName("kotlin.collections"), null, Name.identifier("emptyList"))).single()
            function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
            ) {
                +irReturn(irCall(mutableListOf))
            }
        }
//        irClass.functions.forEach { function ->
//
//        }
//        val baseNode =
//            irClass.getAllSuperclasses().find {
//                it.kotlinFqName.toString() == BaseNode::class.qualifiedName
//            }!!
//        val baseNodeProperties = baseNode.properties.find { it.name.identifier == "properties" }!!
//        val returnType = baseNodeProperties.getter!!.returnType

//        val function = irClass.addFunction("calculateFeatures", returnType, Modality.OPEN, DescriptorVisibilities.PROTECTED,
//            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
//        function.overriddenSymbols = mutableListOf(baseNode.functions.find { it.name.identifier == "calculateFeatures" }!!.symbol)

    }

    private fun overrideCalculateFeatures(irClass: IrClass, pluginContext: IrPluginContext) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateFeatures for ${irClass.name.identifier}",
            irClass.compilerSourceLocation
        )
        irClass.functions.forEach { function ->
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function ${function.name.identifier}",
                irClass.compilerSourceLocation
            )
        }
        val baseNode =
            irClass.getAllSuperclasses().find {
                it.kotlinFqName.toString() == BaseNode::class.qualifiedName
            }!!
        val baseNodeProperties = baseNode.properties.find { it.name.identifier == "properties" }!!
        val returnType = baseNodeProperties.getter!!.returnType

//        val functionSignature = IdSignature.CommonSignature(irClass.kotlinFqName.asString(), "calculateFeatures", null, 0, )
//
//        val function = IrFactoryImpl.createSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
//            Name.identifier("calculateFeatures"), DescriptorVisibilities.PROTECTED, false, false,
//            baseNodeProperties.getter!!.returnType,baseNodeProperties.getter!!.modality,
//            IrSimpleFunctionPublicSymbolImpl(functionSignature),
//            false, false, false, false)
//        val function = irClass.addFunction {
//            name = Name.identifier("calculateFeatures")
//            this.returnType = returnType
//            modality = Modality.OPEN
//            visibility = DescriptorVisibilities.PROTECTED
//            //originalDeclaration = baseNode.functions.find { it.name.identifier == "calculateFeatures" }!!
//            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION
//        }
        val function = irClass.addFunction("calculateFeatures", returnType, Modality.OPEN, DescriptorVisibilities.PROTECTED,
            origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION)
         function.overriddenSymbols = mutableListOf(baseNode.functions.find { it.name.identifier == "calculateFeatures" }!!.symbol)
        val mutableListOf =
            pluginContext
                .referenceFunctions(CallableId(FqName("kotlin.collections"), null, Name.identifier("emptyList"))).single()
        function.body = DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
            IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
        ) {
            +irReturn(irCall(mutableListOf))
        }
    }

    private fun overrideProperties(irClass: IrClass, pluginContext: IrPluginContext) {
        val nodeLike =
            irClass.allSuperInterfaces().find {
                it.kotlinFqName.toString() == NodeLike::class.qualifiedName
            }!!
        val baseNode =
            irClass.getAllSuperclasses().find {
                it.kotlinFqName.toString() == BaseNode::class.qualifiedName
            }!!
        val nodeLikeProperties = nodeLike.properties.find { it.name.identifier == "properties" }!!
        val baseNodeProperties = baseNode.properties.find { it.name.identifier == "properties" }!!
        baseNode.declarations.forEach {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Declaration ${it}",
                irClass.compilerSourceLocation,
            )
        }
        baseNode.functions.forEach {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "Function ${it.name}",
                irClass.compilerSourceLocation,
            )
        }
        //val baseNodeGetProperties = baseNode.functions.find { it.name.identifier == "getProperties" }!!

        //            override val properties: List<FeatureDescription>
//            get() = TODO("Not yet implemented")


        val property = IrFactoryImpl.createProperty(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            Name.identifier("properties"), DescriptorVisibilities.PUBLIC, Modality.OPEN,
            IrPropertySymbolImpl(), false, false, false, false,
            false, null, false, false)
        val accessorSignature =
            IdSignature.CommonSignature(
                packageFqName = irClass.kotlinFqName.asString(),
                declarationFqName = "${irClass.kotlinFqName.asString()}.properties.get",
                id = null,
                mask = 0,
                description = null,
            )
        property.getter = IrFactoryImpl.createSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
            Name.identifier("getProperties")/*baseNodeProperties.getter!!.name*/, DescriptorVisibilities.PUBLIC, false, false,
            baseNodeProperties.getter!!.returnType,baseNodeProperties.getter!!.modality, IrSimpleFunctionPublicSymbolImpl(accessorSignature),
            false, false, false, false)
        property.getter!!.overriddenSymbols = mutableListOf(baseNodeProperties.getter!!.symbol)
        val mutableListOf =
            pluginContext
                .referenceFunctions(CallableId(FqName("kotlin.collections"), null, Name.identifier("emptyList"))).single()
        property.getter!!.body = DeclarationIrBuilder(pluginContext, property.getter!!.symbol).irBlockBody(
            IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
        ) {
            +irReturn(irCall(mutableListOf))
        }
        property.parent = irClass
        irClass.declarations.add(property)

//                val propertySignature = IdSignature.CommonSignature(irClass.kotlinFqName.asString(), "properties", null, 0, )
//                val propertiesSymbol = IrPropertyPublicSymbolImpl(propertySignature)
//                //val propertiesDecl = DeclarationIrBuilder(pluginContext, propertiesSymbol)
//                val origin = IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey)
//                val propertiesDecl = IrPropertyImpl(0, 0, origin, propertiesSymbol, Name.identifier("properties"),
//                    DescriptorVisibilities.PUBLIC, Modality.OPEN, false, false, false, false, false)
//                propertiesDecl.parent = irClass
//                messageCollector.report(
//                    CompilerMessageSeverity.WARNING,
//                    "Getter ${propertiesDecl.getter}",
//                )
//                val accessorSignature =
//                    IdSignature.CommonSignature(
//                        packageFqName = irClass.kotlinFqName.asString(),
//                        declarationFqName = "${irClass.kotlinFqName.asString()}.properties.get",
//                        id = null,
//                        mask = 0,
//                        description = null,
//                    )
//                val propertiesGetterSymbol = IrSimpleFunctionPublicSymbolImpl(IdSignature.AccessorSignature(propertySignature, accessorSignature))
//
//                val listClassifierSymbol : IrClassifierSymbol = List::class.classifierSymbol
//                val featureDescription : IrTypeArgument = IrSimpleTypeImpl(null, FeatureDescription::class.classifierSymbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList())
//                val propertiesType : IrType = IrSimpleTypeImpl(null, listClassifierSymbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, listOf(featureDescription), emptyList())
//                propertiesDecl.getter = IrFunctionImpl(0, 0, origin, propertiesGetterSymbol, Name.special("<get>"),
//                    DescriptorVisibilities.PUBLIC, Modality.OPEN, propertiesType,false, false, false, false, false, false, false)
//                irClass.declarations.add(propertiesDecl)
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

