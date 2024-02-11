package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.kcp.compilerSourceLocation
import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirImplementationDetail
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.FirReturnExpression
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirFunctionCallBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirReturnExpressionBuilder
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.references.FirNamedReference
import org.jetbrains.kotlin.fir.references.builder.FirResolvedCallableReferenceBuilder
import org.jetbrains.kotlin.fir.references.impl.FirSimpleNamedReference
import org.jetbrains.kotlin.fir.resolve.dfa.DfaInternals
import org.jetbrains.kotlin.fir.resolve.dfa.symbol
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class BaseNodeGenerator(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {

    private fun log(text: String) {
        val file = File("/Users/federico/repos/kolasu-mp-example/log.txt")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val current = LocalDateTime.now().format(formatter)
        file.appendText("$current: $text\n")
    }


    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        log("generateTopLevelClassLikeDeclaration $classId")
        return generateTopLevelClassLikeDeclaration(classId)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext
    ): FirClassLikeSymbol<*>? {
        log("generateNestedClassLikeDeclaration $owner $name $context")
        return super.generateNestedClassLikeDeclaration(owner, name, context)
    }

    @OptIn(FirImplementationDetail::class)
    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirNamedFunctionSymbol> {
        log("generateFunctions $callableId $context")
        if (callableId.callableName.identifier == "calculateFeatures") {
            val name = Name.identifier("calculateFeatures")
            val listClassId = ClassId.fromString(List::class.qualifiedName!!.replace(".", "/"))
            val featureDescriptionClassId = ClassId.fromString(FeatureDescription::class.qualifiedName!!.replace(".", "/"))
            val type : ConeKotlinType = listClassId.createConeType(session, typeArguments = arrayOf(featureDescriptionClassId.createConeType(session)))
            val classSymbol = callableId.classId!!.toSymbol(session) as FirClassSymbol<*>
            val function = createMemberFunction(classSymbol, Key, name, type) {
//                this.status {
//                    this.setv
//                    this.visibility = Visibilities.Protected
//                    this.modality = Modality.OPEN
//                    this.effectiveVisibility = EffectiveVisibility.Protected(null)
//                }
            }
            function.replaceBody(FirBlockBuilder()
                .apply {
//                    statements.add(FirReturnExpressionBuilder().apply {
//                        this.target = FirFunctionTarget(null, false).apply {
//                            bind(function)
//                        }
//
//                        // FirSimpleNamedReference(source = null, name=Name.identifier("mutableListOf"))
//                        val mutableListOf = FirResolvedCallableReferenceBuilder().apply {
//                            this.name = Name.identifier("mutableListOf")
//                            this.resolvedSymbol = FirNamedFunctionSymbol(CallableId(FqName("kotlin.collections"), null, Name.identifier("mutableListOf")))
//                            this.mappedArguments = emptyMap()
//                        }.build()
//
//                        this.result = FirFunctionCallBuilder().apply {
//                            this.calleeReference = mutableListOf//
//                            this.argumentList = buildResolvedArgumentList(LinkedHashMap())
//                        }.build()
//                    }.build())
                }
                .build())
            // function.body = FirBlockBuilder()
            return listOf(function.symbol)
        }
        return super.generateFunctions(callableId, context)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?
    ): List<FirPropertySymbol> {
        log("generateProperties $callableId $context")
        return super.generateProperties(callableId, context)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        log("generateConstructors $context")
        return super.generateConstructors(context)
    }

    @OptIn(SymbolInternals::class, DfaInternals::class)
    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>, context: MemberGenerationContext): Set<Name> {
        log("getCallableNamesForClass $classSymbol $context")
        if (classSymbol.extendBaseNode && !classSymbol.isAbstract && !classSymbol.isSealed) {
            log("  ${classSymbol.classId.asSingleFqName().asString()} extends BaseNode")
//            val res = super.getCallableNamesForClass(classSymbol, context)
//            log("  res = $res")
            val name = Name.identifier("calculateFeatures")
//            val listClassId = ClassId.fromString(List::class.qualifiedName!!.replace(".", "/"))
//            val featureDescriptionClassId = ClassId.fromString(FeatureDescription::class.qualifiedName!!.replace(".", "/"))
//            val type : ConeKotlinType = listClassId.createConeType(session, typeArguments = arrayOf(featureDescriptionClassId.createConeType(session)))
//            val function = createMemberFunction(classSymbol, Key, name, type) {
////                this.status {
////                    this.setv
////                    this.visibility = Visibilities.Protected
////                    this.modality = Modality.OPEN
////                    this.effectiveVisibility = EffectiveVisibility.Protected(null)
////                }
//            }

            return setOf(name)
        }
        return super.getCallableNamesForClass(classSymbol, context)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext
    ): Set<Name> {
        log("getNestedClassifiersNames $classSymbol $context")
        return super.getNestedClassifiersNames(classSymbol, context)
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
        log("getTopLevelClassIds")
        return super.getTopLevelClassIds()
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        log("hasPackage $packageFqName")
        return super.hasPackage(packageFqName)
    }

    object Key : GeneratedDeclarationKey()
}

@OptIn(SymbolInternals::class)
val FirClassSymbol<*>.extendBaseNode : Boolean
    get() = this.fir.superTypeRefs.any {
    (it as? FirResolvedTypeRefImpl)?.type?.classId?.asSingleFqName()?.asString() == BaseNode::class.qualifiedName!!
}