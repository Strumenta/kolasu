package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.kcp.classId
import com.strumenta.kolasu.kcp.fir.MPNodesCollector.knownMPNodeSubclasses
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.model.KolasuGen
import com.strumenta.kolasu.model.MPNode
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.firClassLike
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.resolve.transformers.ensureResolvedTypeDeclaration
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.types.impl.FirUserTypeRefImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

const val GENERATED_CALCULATE_CONCEPT = "calculateConcept"

class MPNodeGenerator(
    session: FirSession,
) : BaseFirExtension(session) {
    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        log("generateTopLevelClassLikeDeclaration $classId")
        return super.generateTopLevelClassLikeDeclaration(classId)
    }

    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? {
        log("generateNestedClassLikeDeclaration $owner $name $context")
        return super.generateNestedClassLikeDeclaration(owner, name, context)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        log("generateFunctions $callableId $context")
        if (callableId.callableName.identifier == GENERATED_CALCULATE_CONCEPT) {
            val name = Name.identifier(GENERATED_CALCULATE_CONCEPT)
            val type: ConeKotlinType = ClassId.fromString(Concept::class.qualifiedName!!.replace(".", "/")).toConeType()
            val classSymbol = callableId.classId!!.toSymbol(session) as FirClassSymbol<*>
            val function =
                createMemberFunction(classSymbol, Key, name, type) {
                }
            function.replaceBody(
                FirBlockBuilder()
                    .build(),
            )
            return listOf(function.symbol)
        }
        return super.generateFunctions(callableId, context)
    }

    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        log("generateProperties $callableId $context")
        return super.generateProperties(callableId, context)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        log("generateConstructors $context")
        return super.generateConstructors(context)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        log("getCallableNamesForClass $classSymbol $context")
        if (classSymbol.extendMPNode(session)) {
            log("  ${classSymbol.classId.asSingleFqName().asString()} extends MPNode")
            knownMPNodeSubclasses.add(classSymbol.name)
            if (!classSymbol.isAbstract && !classSymbol.isSealed) {
                val set =
                    mutableSetOf(
                        Name.identifier(GENERATED_CALCULATE_CONCEPT),
                    )
                return set
            }
        }
        return super.getCallableNamesForClass(classSymbol, context)
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        log("getNestedClassifiersNames $classSymbol $context")
        return super.getNestedClassifiersNames(classSymbol, context)
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
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
fun FirClassSymbol<*>.isOrExtendMPNode(firSession: FirSession): Boolean =
    isMPNode(firSession) || extendMPNode(firSession)

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.isMPNode(firSession: FirSession): Boolean =
    this.classId.asSingleFqName().asString() == MPNode::class.qualifiedName!!

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.isKolasuGenEnabled(firSession: FirSession): Boolean {
    return this.extendMPNode(firSession) || this.getAnnotationByClassId(KolasuGen::class.classId, firSession) != null
//    return this.extendMPNode(firSession) || this.annotations.any {
//        val classId = it.annotationTypeRef.firClassLike(firSession)?.classId
//        if (classId == null) {
//            val coneType = it.annotationTypeRef.coneTypeOrNull
//            if (coneType == null) {
//                val classSymbol = it.annotationTypeRef.toClassLikeSymbol(firSession)
//                if (classSymbol == null) {
//                    it.annotationTypeRef
//                } else {
//                    classSymbol.classId.asSingleFqName().asString() == KolasuGen::class.qualifiedName
//                }
//            } else {
//                coneType.classId?.asSingleFqName()?.asString() == KolasuGen::class.qualifiedName
//            }
//        } else {
//            classId.asSingleFqName().asString() == KolasuGen::class.qualifiedName
//        }
//    }
}

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.extendMPNode(firSession: FirSession): Boolean =
    this.fir.superTypeRefs.any {
        when (it) {
            is FirResolvedTypeRefImpl -> {
                (it.type.classId!!.toSymbol(firSession) as FirClassSymbol<*>).isOrExtendMPNode(firSession)
            }

            is FirImplicitAnyTypeRef -> {
                false
            }

            is FirErrorTypeRef -> {
                false
            }

            is FirUserTypeRefImpl -> {
                if (it.qualifier.any { it.name.identifier == "MPNode" }) {
                    true
                } else {
                    it.ensureResolvedTypeDeclaration(firSession)
                    val classLike = it.firClassLike(firSession)
                    if (classLike == null) {
                        MPNodesCollector.knownMPNodeSubclasses.any { sc ->
                            it.qualifier.any { it.name.identifier == sc.identifier }
                        }
                    } else {
                        TODO()
                    }
                }
            }

            else -> {
                throw IllegalStateException("Processing ${it.javaClass.canonicalName} ($it)")
            }
        }
    }
