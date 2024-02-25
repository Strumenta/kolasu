@file:OptIn(FirIncompatiblePluginAPI::class, ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import com.strumenta.kolasu.model.FeatureType
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.Node
import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.backend.js.utils.typeArguments
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.declarations.buildValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildVariable
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
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
                    // val name: String,
                    // val provideNodes: Boolean,
                    // val multiplicity: Multiplicity,
                    // val valueProvider: () -> Any?,
                    // val featureType: FeatureType,
                    // val derived: Boolean = false,
                    putValueArgument(0, irString(property.name.identifier))
                    putValueArgument(1, irBoolean(false))
                    putValueArgument(
                        2,
                        irCall(multiplicityValueOf).apply {
                            putValueArgument(0, irString("SINGULAR"))
                        },
                    )
                    //                                val getter = irClass.functions.find { it.name.identifier == "get${property.name.identifier.capitalize()}"} !!
                    //                                IrSimpleFunctionSymbolImpl().apply {
                    //                                    this.
                    //                                }
                    // val getter = property.getter!!
                    // val getter = pluginContext.referenceFunctions(CallableId(irClass.classId!!, Name.identifier("get${property.name.identifier.capitalize()}"))).single()
                    // putValueArgument(3, irFunctionReference(irClass.defaultType, getter.symbol))

                    // I need to pass to it the actual node I guess
                    val lambda =
                        context
                            .irFactory
                            .buildFun {
                                startOffset = SYNTHETIC_OFFSET
                                endOffset = SYNTHETIC_OFFSET
                                origin =
                                    IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
                                name = Name.special("<anonymous>")
                                visibility = DescriptorVisibilities.LOCAL
                                returnType = pluginContext.irBuiltIns.anyType
                            }.apply {
                                parent = function
                                require(symbol.owner.file == irClass.file)
//                                        valueParameters = listOf (
//                                            buildValueParameter(this) {
//                                                origin = IrDeclarationOrigin.DEFINED
//                                                name = Name.identifier("field")
//                                                index = 0
//                                                type = irClass.defaultType
//                                            }
//                                        )
                                println("TYPE OF function.dispatchReceiverParameter -> ${(function.dispatchReceiverParameter!!.type as IrSimpleType).classifier}")
                                println("property.backingField -> ${property.backingField}")
                                body =
                                    irBlockBody {

                                        HERE RETURN HAS TO BE FIXED

                                        +irNull()
                                        //+irReturn(irNull())
                                        // TODO UNCOMMENT ME!
//                                        +irReturn(
//                                            irGetField(
//                                                irGet(
//                                                    function
//                                                        .dispatchReceiverParameter!!,
//                                                ),
//                                                property.backingField
//                                                    ?: throw IllegalStateException(
//                                                        "no backing field for property ${property.name.identifier} " +
//                                                            "in ${irClass.kotlinFqName.asString()}",
//                                                    ),
//                                            ),
//                                        )
                                    }
                            }

                    // MAYBE I NEED TO ADD THE LAMBDA?
                    // irClass.addMember(lambda)

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(0)
                                    .typeWith(pluginContext.irBuiltIns.anyType),
                            origin = IrStatementOrigin.LAMBDA,
                            function = lambda,
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
            //println("TYPE OF CONSTRUCTOR CALL ${(constructorCall.type as IrSimpleType).classifier.getClassFqNameUnsafe().asString()}")
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
        val function = irClass.functions.find { it.name.identifier == "calculateFeatures" }
        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function calculateFeatures FOUND",
                irClass.compilerSourceLocation,
            )
            val mutableListOf =
                pluginContext
                    .referenceFunctions(
                        CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableListOf")),
                    ).find {
                        it.owner!!.valueParameters.size == 1
                    }!!
            function.body =
                DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                ) {
                    // We create a mutable list with no parameters
                    // val mutableListOfCall = irCall(mutableListOf)
                    // Then we call apply on it
                    // apply {  }

                    val mutableListOfZeroParams =
                        pluginContext
                            .referenceFunctions(
                                CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableListOf")),
                            ).find {
                                it.owner!!.valueParameters.isEmpty()
                            }!!

                    val emptyCall = IrCallImpl(
                    startOffset, endOffset, mutableListOfZeroParams.owner.returnType, mutableListOfZeroParams,
                    typeArgumentsCount = 1,
                    valueArgumentsCount = 0,
                    origin = null
                ).apply {
                    this.putTypeArgument(0, pluginContext.referenceClass(FeatureDescription::class.classId)!!.defaultType)
                }

                    val call = IrCallImpl(
                        startOffset, endOffset, mutableListOf.owner.returnType, mutableListOf,
                        typeArgumentsCount = 1,
                        valueArgumentsCount = 1,
                        origin = null
                    ).apply {
                            this.putTypeArgument(0, pluginContext.referenceClass(FeatureDescription::class.classId)!!.defaultType)
                            val param = mutableListOf.owner!!.valueParameters.first()
                            val values = buildList {
                                irClass.properties.forEach { property ->
                                    populateFeatureListWithProperty(
                                        property, function,
                                        irClass, pluginContext,
                                    ) {
                                        add(it)
                                    }
                                }
                            }
                            putValueArgument(
                                0,
                                irVararg(
                                    param.type,
                                    values,
                                ),
                            )
                        }
//                val mutableListClass = pluginContext.referenceClass(MutableList::class.classId) ?: throw RuntimeException("MutableList not found")
//                val add = mutableListClass.functions.find { it.owner.name.identifier == "add" } ?: throw RuntimeException("MutableList.add not found")
//                irClass.properties.forEach { property ->
//                    +irCall(add).apply {
//                        dispatchReceiver = irGet(variable)

//                        class FeatureDescription(
//                            val name: String,
//                            val provideNodes: Boolean,
//                            val multiplicity: Multiplicity,
//                            val valueProvider: () -> Any?,
//                            val featureType: FeatureType,
//                            val derived: Boolean = false,

//                            putValueArgument(0, irNull())
//                    }
//                }

                    val listClass = pluginContext.referenceClass(MutableList::class.classId)!!
                    val listOfFeatureDescriptionType = listClass.typeWith(pluginContext.referenceClass(FeatureDescription::class.classId)!!.defaultType)
//                    val resultVariable = buildVariable(function, SYNTHETIC_OFFSET, SYNTHETIC_OFFSET, myOrigin,
//                        Name.identifier("features"), listOfFeatureDescriptionType).apply {
//                            initializer = emptyCall
//                    }
                    //+irSet(resultVariable, emptyCall)
                    val resultVariable = irTemporary(emptyCall, nameHint = "myFeatures", listOfFeatureDescriptionType)
                    //+resultVariable

                    irClass.properties.forEach { property ->
                        populateFeatureListWithProperty(
                            property, function,
                            irClass, pluginContext,
                        ) { featureDescription ->
                            // We want to invoke add
//                            val mutableCollection = pluginContext.referenceClass(MutableCollection::class.classId)!!
//                            //println("BY NAME " + mutableCollection.functionByName("add"))
//                            listClass.functions.forEach {
//                                println("LIST CLASS FUNCTION $it ${it.owner.name}")
//                            }
//                            pluginContext.referenceFunctions()
                            // val addMethod = mutableCollection.functions.find { it.owner.name.identifier == "add" && it.owner.valueParameters.size == 1}!!
                            val addMethod = pluginContext
                                    .referenceFunctions(
                            CallableId(FqName("kotlin.collections"), FqName.topLevel(Name.identifier("MutableList")), Name.identifier("add")),
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