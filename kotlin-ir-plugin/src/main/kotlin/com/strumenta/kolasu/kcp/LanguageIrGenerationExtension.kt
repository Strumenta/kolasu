package com.strumenta.kolasu.kcp

import com.strumenta.kolasu.language.Classifier
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.language.EnumType
import com.strumenta.kolasu.language.Property
import com.strumenta.kolasu.language.Reference
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.model.Internal
import com.strumenta.kolasu.model.Multiplicity
import com.strumenta.kolasu.model.NodeLike
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irBlockBody
import org.jetbrains.kotlin.backend.jvm.codegen.AnnotationCodegen.Companion.annotationClass
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.createType
import org.jetbrains.kotlin.ir.types.isMarkedNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SYNTHETIC_OFFSET
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext.getClassFqNameUnsafe

class LanguageIrGenerationExtension(
    private val messageCollector: MessageCollector,
) : BaseIrGenerationExtension() {
    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun generateLanguageInitializer(
        pluginContext: IrPluginContext,
        languageClass: IrClass,
        astClasses: List<IrClass>,
        enumClasses: List<IrClass>
    ) {
        val languageInitializer =
            IrFactoryImpl.createAnonymousInitializer(
                languageClass.startOffset,
                languageClass.endOffset,
                IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey),
                IrAnonymousInitializerSymbolImpl(),
                false,
            )
        languageInitializer.parent = languageClass

        languageInitializer.body =
            DeclarationIrBuilder(pluginContext, languageInitializer.symbol).irBlockBody(
                IrFactoryImpl.createBlockBody(languageClass.startOffset, languageClass.endOffset),
            ) {
                // producing:
                // types.add(Concept(this, "Foo"))
                val types = languageClass.properties.find { it.name.identifier == StarLasuLanguage::types.name }!!
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
                            it.owner.valueParameters.size == 1
                        }!!

                // We add the enums
                enumClasses.forEach { enumClass ->
                    val enumTypeConstructor = pluginContext.referenceConstructors(EnumType::class.classId).first()
                    val enumTypeInstance =
                        createTmpVariable(
                            irCallConstructor(enumTypeConstructor, emptyList()).apply {
                                putValueArgument(0, irString(enumClass.name.identifier))
                            },
                        )
                    +irCall(addMethod).apply {
                        dispatchReceiver =
                            irCall(types.getter!!).apply {
                                dispatchReceiver = irGetObject(languageClass.symbol)
                            }
                        putValueArgument(
                            0,
                            irGet(enumTypeInstance),
                        )
                    }
                }

                // We first instantiate the concepts, without populating them (as their features could refer
                // to themselves or other types declared in the language)
                astClasses.forEach { astClass ->
                    val conceptConstructor = pluginContext.referenceConstructors(Concept::class.classId).first()
                    val conceptInstance =
                        createTmpVariable(
                            irCallConstructor(conceptConstructor, emptyList()).apply {
                                putValueArgument(0, irGetObject(languageClass.symbol))
                                putValueArgument(1, irString(astClass.name.identifier))
                            },
                        )
                    +irCall(addMethod).apply {
                        dispatchReceiver =
                            irCall(types.getter!!).apply {
                                dispatchReceiver = irGetObject(languageClass.symbol)
                            }
                        putValueArgument(
                            0,
                            irGet(conceptInstance),
                        )
                    }
                }
                // We then populate the types
                astClasses.forEach { astClass ->
                    // get the concept instance from the language
                    val languageInstance = irGetObject(languageClass.symbol)
                    val getConceptLike =
                        pluginContext
                            .referenceClass(StarLasuLanguage::class.classId)!!
                            .functionByName(StarLasuLanguage::getConceptLike.name)
                    val conceptLikeInstance =
                        createTmpVariable(
                            irCall(getConceptLike).apply {
                                dispatchReceiver = languageInstance
                                putValueArgument(0, irString(astClass.name.identifier))
                            },
                        )
                    // +conceptInstance
                    astClass.properties.forEach { property ->
                        val isInternal =
                            property.annotations.any {
                                it.annotationClass.kotlinFqName.asString() == Internal::class.qualifiedName
                            }
                        if (!isInternal) {
                            val type = property.getter!!.returnType!!.classifierOrNull!!
                            if (property.declareSingleOrOptionalContainment()) {
                                +handleSingleOrOptionalContainment(
                                    pluginContext, property, languageClass,
                                    languageInitializer, addMethod, conceptLikeInstance,
                                )
                            } else if (property.declareSingleOrOptionalAttribute()) {
                                +handleSingleOrOptionalAttribute(
                                    pluginContext, property, languageClass,
                                    languageInitializer, addMethod, conceptLikeInstance,
                                )
                            } else if (property.declareSingleOrOptionalReference()) {
                                +handleSingleOrOptionalReference(
                                    pluginContext, property, languageClass,
                                    languageInitializer, addMethod, conceptLikeInstance,
                                )
                            } else if (property.declareMultipleContainment()) {
                                +handleMultipleContainment(
                                    pluginContext, property, languageClass,
                                    languageInitializer, addMethod, conceptLikeInstance,
                                )
                            } else {
                                val propertyType = property.backingField?.type ?: property.getter?.returnType
                                val assignableToList = propertyType?.isAssignableTo(List::class)
                                val assignableToNodeLike = propertyType?.isAssignableTo(NodeLike::class)
                                TODO(
                                    "Unable to process property " +
                                        "${property.fqNameWhenAvailable?.asString() ?: property.name} " +
                                        "propertyType=${propertyType?.classFqName} " +
                                        "assignableToList=$assignableToList " +
                                        "assignableToNodeLike=$assignableToNodeLike",
                                )
                            }
                        }
                    }
                }
            }
        languageClass.declarations.add(languageInitializer)
    }

    private fun IrBuilderWithScope.handleSingleOrOptionalAttribute(
        pluginContext: IrPluginContext,
        property: IrProperty,
        languageClass: IrClass,
        languageInitializer: IrAnonymousInitializer,
        addMethod: IrSimpleFunctionSymbol,
        conceptInstance: IrVariable,
    ): IrStatement {
        // c.features.add(Attribute("attrName", optional, dataType, valueProvider, derived))
        return irCall(addMethod).apply {
            // c.features
            val featuresField = pluginContext.conceptLikeFeaturesField()
            dispatchReceiver =
                irCall(featuresField.getter!!).apply {
                    dispatchReceiver = irGet(conceptInstance)
                }
            val attributeConstructor =
                pluginContext.referenceConstructors(Property::class).first()
            val getDataType =
                pluginContext
                    .referenceClass(StarLasuLanguage::class.classId)!!
                    .functionByName(StarLasuLanguage::getDataType.name)
            putValueArgument(
                0,
                irCallConstructor(attributeConstructor, emptyList()).apply {
                    // name
                    putValueArgument(0, irString(property.name.identifier))
                    // optional
                    putValueArgument(1, irBoolean(false))
                    // data type: language.getDataType("dataTypeName")
                    putValueArgument(
                        2,
                        irCall(getDataType).apply {
                            dispatchReceiver = irGetObject(languageClass.symbol)
                            putValueArgument(
                                0,
                                irString(
                                    property
                                        .getter!!
                                        .returnType
                                        .classFqName!!
                                        .shortName().asString(),
                                ),
                            )
                        },
                    )
                    // value provider
                    // To create lambda you should do something like this:
                    // 1. Create a simple function with LOCAL_FUNCTION_FOR_LAMBDA origin (ofc, fill it with types you need):
                    //     https://github.com/ForteScarlet/kotlin-suspend-transform-compiler-plugin/blob/
                    //           2c3bf967fdc81e20fc73ac90e8e54ce51833d35b/compiler/suspend-transform-plugin/src/main/kotlin/
                    //           love/forte/plugin/suspendtrans/utils/IrFunctionUtils.kt#L147
                    // 2. Create IrFunctionExpression IR-node, that accepts the previously created simple function and its type.
                    //    As far as I know, there is no IrBuilderWithScope method for this type of IR-node, but this is how we represent
                    //    lambdas inside the compiler, so you can create it with the IrFunctionExpressionImpl constructor.

                    val lambda =
                        pluginContext.createLambdaFunctionWithNeededScope(
                            languageClass,
                            languageInitializer,
                            property,
                        )

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(1)
                                    .typeWith(
                                        pluginContext
                                            .referenceClass(NodeLike::class.classId)!!
                                            .createType(false, emptyList()),
                                        pluginContext
                                            .irBuiltIns
                                            .anyNType,
                                    ),
                            origin = IrStatementOrigin.LAMBDA,
                            function = lambda,
                        ),
                    )
                    // derived
                    putValueArgument(4, irBoolean(false))
                },
            )
        }
    }

    private fun IrBuilderWithScope.handleSingleOrOptionalReference(
        pluginContext: IrPluginContext,
        property: IrProperty,
        languageClass: IrClass,
        languageInitializer: IrAnonymousInitializer,
        addMethod: IrSimpleFunctionSymbol,
        conceptInstance: IrVariable,
    ): IrStatement {
        // c.features.add(Reference("referenceName", optional, classifier, valueProvider, derived))
        return irCall(addMethod).apply {
            // c.features
            val featuresField = pluginContext.conceptLikeFeaturesField()
            dispatchReceiver =
                irCall(featuresField.getter!!).apply {
                    dispatchReceiver = irGet(conceptInstance)
                }
            val referenceConstructor =
                pluginContext.referenceConstructors(Reference::class).first()
            val getConceptLike =
                pluginContext
                    .referenceClass(StarLasuLanguage::class.classId)!!
                    .functionByName(StarLasuLanguage::getConceptLike.name)
            putValueArgument(
                0,
                irCallConstructor(referenceConstructor, emptyList()).apply {
                    // name
                    putValueArgument(0, irString(property.name.identifier))
                    // optional
                    putValueArgument(1, irBoolean(false))
                    // data type: language.getConceptLike("classifierName")
                    putValueArgument(
                        2,
                        irCall(getConceptLike).apply {
                            dispatchReceiver = irGetObject(languageClass.symbol)
                            putValueArgument(
                                0,
                                irString(
                                    // Here we take the type parameter ReferenceValue<MyAstClass>
                                    (property
                                        .getter!!
                                        .returnType as IrSimpleType).arguments[0].typeOrNull!!
                                        .classFqName!!.shortName()
                                        .asString(),
                                ),
                            )
                        },
                    )
                    // value provider
                    // To create lambda you should do something like this:
                    // 1. Create a simple function with LOCAL_FUNCTION_FOR_LAMBDA origin (ofc, fill it with types you need):
                    //     https://github.com/ForteScarlet/kotlin-suspend-transform-compiler-plugin/blob/
                    //           2c3bf967fdc81e20fc73ac90e8e54ce51833d35b/compiler/suspend-transform-plugin/src/main/kotlin/
                    //           love/forte/plugin/suspendtrans/utils/IrFunctionUtils.kt#L147
                    // 2. Create IrFunctionExpression IR-node, that accepts the previously created simple function and its type.
                    //    As far as I know, there is no IrBuilderWithScope method for this type of IR-node, but this is how we represent
                    //    lambdas inside the compiler, so you can create it with the IrFunctionExpressionImpl constructor.

                    val lambda =
                        pluginContext.createLambdaFunctionWithNeededScope(
                            languageClass,
                            languageInitializer,
                            property,
                        )

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(1)
                                    .typeWith(
                                        pluginContext
                                            .referenceClass(NodeLike::class.classId)!!
                                            .createType(false, emptyList()),
                                        pluginContext
                                            .irBuiltIns
                                            .anyNType,
                                    ),
                            origin = IrStatementOrigin.LAMBDA,
                            function = lambda,
                        ),
                    )
                    // derived
                    putValueArgument(4, irBoolean(false))
                },
            )
        }
    }

    private fun IrPluginContext.conceptLikeFeaturesField(): IrProperty {
        return this
            .referenceClass(Classifier::class.classId)!!
            .owner
            .properties
            .find { it.name.identifier == "declaredFeatures" }!!
    }

    private fun IrBuilderWithScope.handleSingleOrOptionalContainment(
        pluginContext: IrPluginContext,
        property: IrProperty,
        languageClass: IrClass,
        languageInitializer: IrAnonymousInitializer,
        addMethod: IrSimpleFunctionSymbol,
        conceptInstance: IrVariable,
    ): IrStatement {
        // c.features.add(Containment("attrName", optional, dataType, valueProvider, derived))
        return irCall(addMethod).apply {
            // c.features
            val featuresField = pluginContext.conceptLikeFeaturesField()
            dispatchReceiver =
                irCall(featuresField.getter!!).apply {
                    dispatchReceiver = irGet(conceptInstance)
                }
            val featureConstructor =
                pluginContext.referenceConstructors(Containment::class).first()
            val getConceptLike =
                pluginContext
                    .referenceClass(StarLasuLanguage::class.classId)!!
                    .functionByName(StarLasuLanguage::getConceptLike.name)
            val multiplicityName =
                if (property.getter!!.returnType.isMarkedNullable()) {
                    Multiplicity.OPTIONAL
                } else {
                    Multiplicity.SINGULAR
                }.name
            val multiplicityClass = pluginContext.referenceClass(Multiplicity::class.classId)
            val multiplicityValueOf = multiplicityClass!!.owner.functions.find { it.name.identifier == "valueOf" }!!
            val multiplicity =
                irCall(multiplicityValueOf).apply {
                    putValueArgument(0, irString(multiplicityName))
                }
            putValueArgument(
                0,
                irCallConstructor(featureConstructor, emptyList()).apply {
                    // name
                    putValueArgument(0, irString(property.name.identifier))
                    // multiplicity
                    putValueArgument(1, multiplicity)
                    // type: language.getConceptLike("conceptLikeName")
                    putValueArgument(
                        2,
                        irCall(getConceptLike).apply {
                            dispatchReceiver = irGetObject(languageClass.symbol)
                            putValueArgument(
                                0,
                                irString(
                                    property
                                        .getter!!
                                        .returnType
                                        .classFqName!!
                                        .shortName()
                                        .identifier,
                                ),
                            )
                        },
                    )
                    // value provider
                    // To create lambda you should do something like this:
                    // 1. Create a simple function with LOCAL_FUNCTION_FOR_LAMBDA origin (ofc, fill it with types you need):
                    //     https://github.com/ForteScarlet/kotlin-suspend-transform-compiler-plugin/blob/
                    //           2c3bf967fdc81e20fc73ac90e8e54ce51833d35b/compiler/suspend-transform-plugin/src/main/kotlin/
                    //           love/forte/plugin/suspendtrans/utils/IrFunctionUtils.kt#L147
                    // 2. Create IrFunctionExpression IR-node, that accepts the previously created simple function and its type.
                    //    As far as I know, there is no IrBuilderWithScope method for this type of IR-node, but this is how we represent
                    //    lambdas inside the compiler, so you can create it with the IrFunctionExpressionImpl constructor.

                    val lambda =
                        pluginContext.createLambdaFunctionWithNeededScope(
                            languageClass,
                            languageInitializer,
                            property,
                        )

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(1)
                                    .typeWith(
                                        pluginContext
                                            .referenceClass(NodeLike::class.classId)!!
                                            .createType(false, emptyList()),
                                        pluginContext
                                            .irBuiltIns
                                            .anyNType,
                                    ),
                            origin = IrStatementOrigin.LAMBDA,
                            function = lambda,
                        ),
                    )
                    // derived
                    putValueArgument(4, irBoolean(false))
                },
            )
        }
    }

    @OptIn(ObsoleteDescriptorBasedAPI::class)
    private fun IrBuilderWithScope.handleMultipleContainment(
        pluginContext: IrPluginContext,
        property: IrProperty,
        languageClass: IrClass,
        languageInitializer: IrAnonymousInitializer,
        addMethod: IrSimpleFunctionSymbol,
        conceptInstance: IrVariable,
    ): IrStatement {
        // producing:
        // c.features.add(Containment("attrName", multiplicity, dataType, valueProvider, derived))
        return irCall(addMethod).apply {
            // producing:
            // c.features
            val featuresField = pluginContext.conceptLikeFeaturesField()
            dispatchReceiver =
                irCall(featuresField.getter!!).apply {
                    dispatchReceiver = irGet(conceptInstance)
                }
            val featureConstructor =
                pluginContext.referenceConstructors(Containment::class).first()
            val getConceptLike =
                pluginContext
                    .referenceClass(StarLasuLanguage::class.classId)!!
                    .functionByName(StarLasuLanguage::getConceptLike.name)
            val multiplicity = irExpression(Multiplicity.MANY, pluginContext)
            putValueArgument(
                0,
                irCallConstructor(featureConstructor, emptyList()).apply {
                    // producing:
                    // name
                    putValueArgument(0, irString(property.name.identifier))
                    // producing:
                    // multiplicity
                    putValueArgument(1, multiplicity)
                    // producing:
                    // type: language.getConceptLike("conceptLikeName")
                    putValueArgument(
                        2,
                        irCall(getConceptLike).apply {
                            dispatchReceiver = irGetObject(languageClass.symbol)
                            val typeName =
                                property
                                    .getter!!
                                    .returnType
                                    .toKotlinType()
                                    .arguments[0]
                                    .type
                                    .constructor
                                    .getClassFqNameUnsafe()
                                    .shortName()
                                    .identifier
                            putValueArgument(
                                0,
                                irString(
                                    typeName,
                                ),
                            )
                        },
                    )
                    // value provider
                    // To create lambda you should do something like this:
                    // 1. Create a simple function with LOCAL_FUNCTION_FOR_LAMBDA origin (ofc, fill it with types you need):
                    //     https://github.com/ForteScarlet/kotlin-suspend-transform-compiler-plugin/blob/
                    //           2c3bf967fdc81e20fc73ac90e8e54ce51833d35b/compiler/suspend-transform-plugin/src/main/kotlin/
                    //           love/forte/plugin/suspendtrans/utils/IrFunctionUtils.kt#L147
                    // 2. Create IrFunctionExpression IR-node, that accepts the previously created simple function and its type.
                    //    As far as I know, there is no IrBuilderWithScope method for this type of IR-node, but this is how we represent
                    //    lambdas inside the compiler, so you can create it with the IrFunctionExpressionImpl constructor.

                    val lambda =
                        pluginContext.createLambdaFunctionWithNeededScope(
                            languageClass,
                            languageInitializer,
                            property,
                        )

                    putValueArgument(
                        3,
                        IrFunctionExpressionImpl(
                            startOffset = SYNTHETIC_OFFSET,
                            endOffset = SYNTHETIC_OFFSET,
                            type =
                                pluginContext
                                    .irBuiltIns
                                    .functionN(1)
                                    .typeWith(
                                        pluginContext
                                            .referenceClass(NodeLike::class.classId)!!
                                            .createType(false, emptyList()),
                                        pluginContext
                                            .irBuiltIns
                                            .anyNType,
                                    ),
                            origin = IrStatementOrigin.LAMBDA,
                            function = lambda,
                        ),
                    )
                    // derived
                    putValueArgument(4, irBoolean(false))
                },
            )
        }
    }

    private fun generateConceptMethodForAstClass(
        pluginContext: IrPluginContext,
        languageClass: IrClass,
        astClass: IrClass,
    ) {
        val companions = astClass.declarations.filterIsInstance<IrClass>().filter { it.isCompanion }
        val companionIrClass =
            companions.find { it.properties.any { it.name.identifier == companionConceptPropertyName } }
                ?: throw IllegalStateException(
                    "Cannot find companion with expected property ${companionConceptPropertyName} in ${astClass.kotlinFqName.asString()}. " +
                        "Companions found: ${companions.size}. Companions have these properties: ${
                            companions.joinToString(
                                "; "
                            ) { it.properties.map { it.name }.joinToString(", ") }
                        }",
                )
        val anonymousInitializerSymbolImpl =
            IrFactoryImpl.createAnonymousInitializer(
                astClass.startOffset,
                astClass.endOffset,
                IrDeclarationOrigin.GeneratedByPlugin(StarLasuGeneratedDeclarationKey),
                IrAnonymousInitializerSymbolImpl(),
                false,
            )
        anonymousInitializerSymbolImpl.body =
            DeclarationIrBuilder(pluginContext, anonymousInitializerSymbolImpl.symbol).irBlockBody(
                IrFactoryImpl.createBlockBody(
                    astClass.startOffset,
                    astClass.endOffset,
                ),
            ) {
                val conceptProperty = companionIrClass.conceptProperty
                val starLasulanguage = pluginContext.referenceClass(StarLasuLanguage::class.classId)!!
                val getConcept = starLasulanguage.functionByName(StarLasuLanguage::getConcept.name)
                val conceptValue: IrExpression =
                    irCall(getConcept).apply {
                        dispatchReceiver = irGetObject(languageClass.symbol)
                        putValueArgument(0, irString(astClass.name.asString()))
                    }

                val thisCompanion = irGet(companionIrClass.thisReceiver!!)
                val stmt: IrStatement = irSetField(thisCompanion, conceptProperty.backingField!!, conceptValue)
                +stmt
            }
        anonymousInitializerSymbolImpl.parent = astClass
        companionIrClass.declarations.add(anonymousInitializerSymbolImpl)
    }

    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val astClasses = mutableListOf<IrClass>()
        val languages = mutableListOf<IrClass>()
        val enumClasses = mutableListOf<IrClass>()
        processMPNodeSubclasses(moduleFragment, { astClasses.add(it) }, { languages.add(it) }, {
            enumClasses.add(it)})
        if (astClasses.isNotEmpty()) {
            require(languages.size == 1) {
                if (languages.size == 0) {
                    "We found AST classes in package ${astClasses.first().packageFqName} but no " +
                        "object extending ${StarLasuLanguage::class.qualifiedName}"
                } else {
                    "We found AST classes in package ${astClasses.first().packageFqName} and more than one " +
                        "object extending ${StarLasuLanguage::class.qualifiedName}"
                }
            }
        }
        if (languages.size == 1) {
            val languageClass = languages.first()
            require(languageClass.kind == ClassKind.OBJECT)
            generateLanguageInitializer(pluginContext, languageClass, astClasses, enumClasses)
            processMPNodeSubclasses(moduleFragment, { irClass ->
                generateConceptMethodForAstClass(pluginContext, languageClass, irClass)
            }, {}, {})
        }
    }
}

fun findLanguageClass(moduleFragment: IrModuleFragment): IrClass {
    val languages = mutableListOf<IrClass>()
    processMPNodeSubclasses(moduleFragment, { }, { languages.add(it) }, {})
    if (languages.size == 1) {
        return languages.first()
    } else {
        throw IllegalStateException("We found ${languages.size} when processing MPNode Subclasses")
    }
}

fun IrPluginContext.createLambdaFunctionWithNeededScope(
    containingClass: IrClass,
    containingInitializer: IrAnonymousInitializer,
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
            val nodeParam =
                addValueParameter(
                    Name.identifier("node"),
                    referenceClass(NodeLike::class.classId)!!.createType(false, emptyList()),
                )
            parent = containingClass
            body =
                irBuiltIns.createIrBuilder(symbol).run {
                    irBlockBody {
                        +irReturn(
                            irCall(property.getter!!).apply {
                                dispatchReceiver = irGet(nodeParam)
                            },
                        )
                    }
                }
        }

fun IrBuilderWithScope.irExpression(
    multiplicity: Multiplicity,
    pluginContext: IrPluginContext,
): IrExpression {
    val multiplicityName = Multiplicity.MANY.name
    val multiplicityClass = pluginContext.referenceClass(Multiplicity::class.classId)
    val multiplicityValueOf = multiplicityClass!!.owner.functions.find { it.name.identifier == "valueOf" }!!
    val multiplicity =
        irCall(multiplicityValueOf).apply {
            putValueArgument(0, irString(multiplicityName))
        }
    return multiplicity
}
