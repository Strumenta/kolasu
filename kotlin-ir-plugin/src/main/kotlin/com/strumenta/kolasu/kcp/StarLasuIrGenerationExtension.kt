@file:OptIn(ObsoleteDescriptorBasedAPI::class, UnsafeDuringIrConstructionAPI::class)

package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.language.PrimitiveType
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguagesRegistry
import com.strumenta.kolasu.model.MPNode
import com.strumenta.kolasu.model.Node
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.nestedClasses
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.name.SpecialNames

class StarLasuIrGenerationExtension(
    private val messageCollector: MessageCollector,
) : IrGenerationExtension {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun checkASTNode(
        irClass: IrClass,
        pluginContext: IrPluginContext,
        isMPNode: Boolean,
        moduleFragment: IrModuleFragment,
    ) {
        irClass.primaryConstructor?.valueParameters?.forEach { param ->
            if (param.isVal() && (param.isSingleOrOptionalContainment() || param.isSingleOrOptionalAttribute())) {
                messageCollector.report(
                    CompilerMessageSeverity.WARNING,
                    "Value param ${irClass.kotlinFqName}.${param.name} is not assignable",
                    param.compilerSourceLocation,
                )
            }
        }
        irClass.accept(FieldObservableExtension(pluginContext, isMPNode), null)
        irClass.accept(SettingParentExtension(pluginContext, messageCollector), null)
        if (isMPNode) {
            if (irClass.modality != Modality.SEALED && irClass.modality != Modality.ABSTRACT) {
                overrideCalculateConceptBody(irClass, pluginContext, moduleFragment)
            }
        }
    }

    /**
     * Return an expression calculating the DataType
     */
    private fun IrBuilderWithScope.dataType(
        pluginContext: IrPluginContext,
        irType: IrType,
    ): IrExpression {
        // We need to treat enums differently
        if (irType.isEnum) {
            TODO()
        } else {
            // TODO get PrimitiveType.companion
            val primitiveTypeClass = pluginContext.referenceClass(PrimitiveType::class.classId)!!
            val primitiveTypeCompanionClass =
                (primitiveTypeClass.owner as IrClass).nestedClasses.find {
                    it.isCompanion
                }!!
            val primitiveTypeCompanionGet =
                primitiveTypeCompanionClass.functions.find {
                    it.name.asString() == PrimitiveType.Companion::get.name
                }!!
            primitiveTypeClass.owner.companionObject()
            // FIXME I think I need somehow to indicate the PrimitiveType Companion Object
            return irCall(primitiveTypeCompanionGet).apply {
                dispatchReceiver = irGetObject(primitiveTypeCompanionClass.symbol)
                // Pass the primitive type name
                putValueArgument(0, irString(irType.classFqName!!.asString()))
            }
        }
    }

    /**
     * Return an expression calculating the ConceptLike
     */
    private fun IrBuilderWithScope.conceptLike(
        pluginContext: IrPluginContext,
        irType: IrType,
    ): IrExpression {
        // In the future we may use annotations to influence this
        val languageName: String = irType.classFqName!!.packageName
        val languagesRegistryClassSymbol = pluginContext.referenceClass(StarLasuLanguagesRegistry::class.classId)!!
        val languageRegistry = irGetObject(languagesRegistryClassSymbol)
        val getLanguage =
            languagesRegistryClassSymbol.functions.find {
                it.descriptor.name.asString() ==
                    "getLanguage"
            }!!
        val language =
            irCall(getLanguage).apply {
                dispatchReceiver = irGetObject(languagesRegistryClassSymbol)
                putValueArgument(0, irString(languageName))
            }

        val starlasuLanguage = pluginContext.referenceClass(StarLasuLanguage::class.classId)!!
        val getConceptLike = starlasuLanguage.functionByName("getConceptLike")

        return irCall(getConceptLike).apply {
            dispatchReceiver = language
            putValueArgument(0, irString(irType.classFqName!!.asString()))
        }

        // TODO 1: get the language instance
//        We should always ask the _current language_ for the types, using their names.
//        The current language knows all the declared types but also all the imported types.
//        We should have something like: myStarLasuLanguage.getConceptLike(name:String): ConceptLike
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
                val isMPNode =
                    irClass.getAllSuperclasses().any {
                        it.kotlinFqName.toString() == MPNode::class.qualifiedName
                    }
                if (isASTNode || isMPNode) {
                    if (isASTNode) {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "AST class ${irClass.kotlinFqName} identified",
                            irClass.compilerSourceLocation,
                        )
                    } else {
                        messageCollector.report(
                            CompilerMessageSeverity.INFO,
                            "MPNode class ${irClass.kotlinFqName} identified",
                            irClass.compilerSourceLocation,
                        )
                    }
                    checkASTNode(irClass, pluginContext, isMPNode, moduleFragment)
                }
            }
        }
    }

    private fun overrideCalculateConceptBody(
        irClass: IrClass,
        pluginContext: IrPluginContext,
        moduleFragment: IrModuleFragment,
    ) {
        messageCollector.report(
            CompilerMessageSeverity.WARNING,
            "overrideCalculateConceptBody for ${irClass.name.identifier}",
            irClass.compilerSourceLocation,
        )
        val function = irClass.functions.find { it.name.identifier == "calculateConcept" }

        if (function != null) {
            messageCollector.report(
                CompilerMessageSeverity.WARNING,
                "function calculateConcept FOUND",
                irClass.compilerSourceLocation,
            )
            function.body =
                DeclarationIrBuilder(pluginContext, function.symbol).irBlockBody(
                    IrFactoryImpl.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET),
                ) {
                    // get language instance
                    val languageClass: IrClass = findLanguageClass(moduleFragment)
                    val languageInstance = irGetObject(languageClass.symbol)
                    // call getConcept
                    val getConcept =
                        pluginContext
                            .referenceClass(StarLasuLanguage::class.classId)!!
                            .functionByName("getConcept")
                    val className = irString(irClass.name.identifier)
                    +irReturn(
                        irCall(getConcept).apply {
                            dispatchReceiver = languageInstance
                            putValueArgument(0, className)
                        },
                    )
                }
        }
    }
}

fun IrPluginContext.createLambdaFunctionWithNeededScope(
    containingFunction: IrFunction,
    property: IrProperty,
): IrSimpleFunction =
    irFactory
        .buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = SpecialNames.NO_NAME_PROVIDED
            visibility = DescriptorVisibilities.LOCAL
            returnType = irBuiltIns.anyNType
            modality = Modality.FINAL
            isSuspend = false
        }.apply {
            parent = containingFunction
            body =
                irBuiltIns.createIrBuilder(symbol).run {
                    irBlockBody {
                        val nodeInstance = irGet(containingFunction.dispatchReceiverParameter!!)
                        +irReturn(
                            irCall(property.getter!!).apply {
                                dispatchReceiver = nodeInstance
                            },
                        )
                    }
                }
        }
