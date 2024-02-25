@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.kcp.fir.GENERATED_CALCULATED_FEATURES
import com.strumenta.kolasu.kcp.fir.pseudoLambdaName
import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.GenericFeatureDescription
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.NodeLike
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

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
                irClass.properties.forEach { property ->
                    overridePseudoLambda(irClass, pluginContext, property)
                }
            }
        }
    }

    private fun IrBuilderWithScope.populateFeatureListWithProperty(
        property: IrProperty,
        function: IrSimpleFunction,
        irClass: IrClass,
        pluginContext: IrPluginContext,
        add: (value: IrExpression) -> Unit,
    ) {
        if (property.backingField != null) {
            val constructor =
                pluginContext
                    .referenceConstructors(FeatureDescription::class.classId)
                    .first()
            val multiplicity =
                pluginContext
                    .referenceClass(Multiplicity::class.classId)!!
            val multiplicityValueOf =
                multiplicity.functions.find {
                    it.owner.name.identifier ==
                        "valueOf"
                }!!
            val featureType =
                pluginContext
                    .referenceClass(FeatureType::class.classId)!!
            val featureTypeValueOf =
                featureType.functions.find {
                    it.owner.name.identifier ==
                        "valueOf"
                }!!

            val constructorCall =
                irCallConstructor(constructor, emptyList()).apply {
                    putValueArgument(0, irString(property.name.identifier))
                    putValueArgument(1, irBoolean(false))
                    putValueArgument(
                        2,
                        irCall(multiplicityValueOf).apply {
                            putValueArgument(0, irString("SINGULAR"))
                        },
                    )

                    val pseudoLambda = irClass.functions.find { it.name.identifier == pseudoLambdaName(property.name.identifier) }!!

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(1)
                                    .typeWith(irClass.defaultType),
                            origin = IrStatementOrigin.LAMBDA,
                            function = pseudoLambda,
                        ),
                    )
                    putValueArgument(
                        4,
                        irCall(featureTypeValueOf).apply {
                            putValueArgument(0, irString("CONTAINMENT"))
                        },
                    )
                    putValueArgument(5, irBoolean(false))
                }
            add(constructorCall)
        }
    }

    private fun overrideCalculateFeaturesBody(
        irClass: IrClass,
        pluginContext: IrPluginContext,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateFeaturesBody for ${irClass.name.identifier}",
            irClass.compilerSourceLocation,
        )
        val function = irClass.functions.find { it.name.identifier == GENERATED_CALCULATED_FEATURES
        }
        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function $GENERATED_CALCULATED_FEATURES FOUND",
                irClass.compilerSourceLocation,
            )
            function.body =
                DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                ) {
                    // We create a mutable list with no parameters
                    // Then we add elements to it
                    val mutableListOfZeroParams =
                        pluginContext
                            .referenceFunctions(
                                CallableId(FqName("kotlin.collections"), null,
                                    Name.identifier("mutableListOf")),
                            ).find {
                                it.owner!!.valueParameters.isEmpty()
                            }!!

                    val emptyCall =
                        IrCallImpl(
                            startOffset, endOffset, mutableListOfZeroParams.owner.returnType, mutableListOfZeroParams,
                            typeArgumentsCount = 1,
                            valueArgumentsCount = 0,
                            origin = null,
                        ).apply {
                            this
                                .putTypeArgument(
                                    0,
                                    pluginContext
                                        .referenceClass(FeatureDescription::class.classId)!!
                                        .defaultType,
                                )
                        }

                    val listClass = pluginContext.referenceClass(MutableList::class.classId)!!
                    val listOfFeatureDescriptionType =
                        listClass
                            .typeWith(pluginContext
                                .referenceClass(FeatureDescription::class.classId)!!
                                .defaultType)
                    val resultVariable = irTemporary(emptyCall, nameHint = "myFeatures", listOfFeatureDescriptionType)

                    irClass.properties.forEach { property ->
                        populateFeatureListWithProperty(
                            property, function,
                            irClass, pluginContext,
                        ) { featureDescription ->
                            // We want to invoke add
                            val addMethod =
                                pluginContext
                                    .referenceFunctions(
                                        CallableId(
                                            FqName("kotlin.collections"),
                                            FqName
                                                .topLevel(Name.identifier("MutableList")),
                                            Name
                                                .identifier("add"),
                                        ),
                                    ).find {
                                        it.owner!!.valueParameters.size == 1
                                    }!!

                            +irCall(addMethod).apply {
                                dispatchReceiver = irGet(resultVariable)
                                putValueArgument(0, featureDescription)
                            }
                        }
                    }

                    +irReturn(irGet(resultVariable))
                }
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function $GENERATED_CALCULATED_FEATURES has been modified",
                irClass.compilerSourceLocation,
            )
        }
    }

    private fun overrideCalculateNodeTypeBody(
        irClass: IrClass,
        pluginContext: IrPluginContext,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateNodeTypeBody for ${irClass.name.identifier}",
            irClass.compilerSourceLocation,
        )
        val function = irClass.functions.find { it.name.identifier == "calculateNodeType" }


        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function calculateNodeType FOUND",
                irClass.compilerSourceLocation,
            )
            function.body =
                DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                ) {
                    +irReturn(irString(irClass.name.identifier))
                }
        }
    }

    private fun overridePseudoLambda(
        irClass: IrClass,
        pluginContext: IrPluginContext,
        property: IrProperty,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overridePseudoLambda for ${irClass.name.identifier} and $property",
            irClass.compilerSourceLocation,
        )
        val function = irClass.functions.find { it.name.identifier == pseudoLambdaName(property.name.identifier) }


        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function overridePseudoLambda ${property.name.identifier} FOUND",
                irClass.compilerSourceLocation,
            )
            function.body =
                DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                ) {
                    +irReturn(irCall(property.getter!!).apply {
                        this.dispatchReceiver = irGet(function.dispatchReceiverParameter!!)
                        // TODO pass receiver?
                    })
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

object myOrigin : IrDeclarationOrigin {
    override val name: String
        get() = "KolasuPlugin"
}


TODO: rifare le lambda guardandao

fun IrPluginContext.createSuspendLambdaFunctionWithCoroutineScope(
    originFunction: IrFunction,
    function: IrFunction
): IrSimpleFunction {
    return irFactory.buildFun {
        origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        name = SpecialNames.NO_NAME_PROVIDED
        visibility = DescriptorVisibilities.LOCAL
        returnType = function.returnType
        modality = Modality.FINAL
        isSuspend = true
    }.apply {
        parent = function
        body = createIrBuilder(symbol).run {
            // don't use expr body, coroutine codegen can't generate for it.
            irBlockBody {
                +irReturn(irCall(originFunction).apply call@{
                    // set arguments
                    function.dispatchReceiverParameter?.also {
                        this@call.dispatchReceiver = irGet(it)
                    }

                    function.extensionReceiverParameter?.also {
                        this@call.extensionReceiver = irGet(it)
                    }

                    for ((index, parameter) in function.valueParameters.withIndex()) {
                        this@call.putValueArgument(index, irGet(parameter))
                    }
                })
            }
        }
    }
}