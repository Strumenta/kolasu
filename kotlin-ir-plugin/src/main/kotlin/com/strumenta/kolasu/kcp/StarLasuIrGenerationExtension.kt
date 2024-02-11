@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.Node
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrPropertyImpl
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrClassPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrPropertyPublicSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionPublicSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.SimpleTypeNullability
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
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

                val baseNodeClass = irClass.getAllSuperclasses().find {
                    it.kotlinFqName.toString() == BaseNode::class.qualifiedName
                }!!
                baseNodeClass.declarations.forEach {
                    messageCollector.report(
                        CompilerMessageSeverity.WARNING,
                        "DECLARATION ${it.javaClass}",
                    )
                }

                //            override val properties: List<FeatureDescription>
//            get() = TODO("Not yet implemented")

                val propertySignature = IdSignature.CommonSignature(irClass.kotlinFqName.asString(), "properties", null, 0, )
                val propertiesSymbol = IrPropertyPublicSymbolImpl(propertySignature)
                //val propertiesDecl = DeclarationIrBuilder(pluginContext, propertiesSymbol)
                val origin = IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey)
                val propertiesDecl = IrPropertyImpl(0, 0, origin, propertiesSymbol, Name.identifier("properties"),
                    DescriptorVisibilities.PUBLIC, Modality.OPEN, false, false, false, false, false)
                propertiesDecl.parent = irClass
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "Getter ${propertiesDecl.getter}",
                )
                val accessorSignature =
                    IdSignature.CommonSignature(
                        packageFqName = irClass.kotlinFqName.asString(),
                        declarationFqName = "${irClass.kotlinFqName.asString()}.properties.get",
                        id = null,
                        mask = 0,
                        description = null,
                    )
                val propertiesGetterSymbol = IrSimpleFunctionPublicSymbolImpl(IdSignature.AccessorSignature(propertySignature, accessorSignature))

                val listClassifierSymbol : IrClassifierSymbol = List::class.classifierSymbol
                val featureDescription : IrTypeArgument = IrSimpleTypeImpl(null, FeatureDescription::class.classifierSymbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, emptyList(), emptyList())
                val propertiesType : IrType = IrSimpleTypeImpl(null, listClassifierSymbol, SimpleTypeNullability.DEFINITELY_NOT_NULL, listOf(featureDescription), emptyList())
                propertiesDecl.getter = IrFunctionImpl(0, 0, origin, propertiesGetterSymbol, Name.special("<get>"),
                    DescriptorVisibilities.PUBLIC, Modality.OPEN, propertiesType,false, false, false, false, false, false, false)
                irClass.declarations.add(propertiesDecl)
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

