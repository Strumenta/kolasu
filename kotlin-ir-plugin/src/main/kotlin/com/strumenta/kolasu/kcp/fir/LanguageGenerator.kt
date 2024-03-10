package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.kcp.classId
import com.strumenta.kolasu.language.Concept
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.ir.backend.js.utils.TODO
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class LanguageGenerator(
    session: FirSession,
) : BaseFirExtension(session) {
//    companion object {
//        val MY_CLASS_ID = ClassId(FqName.fromSegments(listOf("foo", "bar")), Name.identifier("MyClass"))
//    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
//        log("LanguageGenerator.generateTopLevelClassLikeDeclaration $classId")
//        if (classId != MY_CLASS_ID) return null
//        val klass =
//            buildRegularClass {
//                moduleData = session.moduleData
//                origin = Key.origin
//                status =
//                    FirResolvedDeclarationStatusImpl(
//                        Visibilities.Public,
//                        Modality.FINAL,
//                        EffectiveVisibility.Public,
//                    )
//                classKind = ClassKind.CLASS
//                scopeProvider = session.kotlinScopeProvider
//                name = classId.shortClassName
//                symbol = FirRegularClassSymbol(classId)
//                superTypeRefs.add(session.builtinTypes.anyType)
//            }
//        return klass.symbol
        TODO()
    }

    private fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
    }

//    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
//        return emptyList()
// //        val classId = context.owner.classId
// //        require(classId == MY_CLASS_ID)
// //        val constructor =
// //            buildPrimaryConstructor {
// //                resolvePhase = FirResolvePhase.BODY_RESOLVE
// //                moduleData = session.moduleData
// //                origin = Key.origin
// //                returnTypeRef =
// //                    buildResolvedTypeRef {
// //                        type = classId.toConeType()
// //                    }
// //                status =
// //                    FirResolvedDeclarationStatusImpl(
// //                        Visibilities.Public,
// //                        Modality.FINAL,
// //                        EffectiveVisibility.Public,
// //                    )
// //                symbol = FirConstructorSymbol(classId)
// //            }.also {
// //                it.containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
// //            }
// //        return listOf(constructor.symbol)
//    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        return emptyList()
//        val function =
//            buildSimpleFunction {
//                resolvePhase = FirResolvePhase.BODY_RESOLVE
//                moduleData = session.moduleData
//                origin = Key.origin
//                status =
//                    FirResolvedDeclarationStatusImpl(
//                        Visibilities.Public,
//                        Modality.FINAL,
//                        EffectiveVisibility.Public,
//                    )
//                returnTypeRef = session.builtinTypes.stringType
//                name = callableId.callableName
//                symbol = FirNamedFunctionSymbol(callableId)
//                // it's better to use default type on corresponding firClass to handle type parameters
//                // but in this case we know that MyClass don't have any generics
//                dispatchReceiverType = callableId.classId?.toConeType()
//            }
//        return listOf(function.symbol)
    }

    override fun getTopLevelClassIds(): Set<ClassId> {
//        val result = setOf(MY_CLASS_ID)
//        log("LanguageGenerator.getTopLevelClassIds -> $result")
//        return result
        // TODO()
        return emptySet()
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
//        val result = packageFqName == MY_CLASS_ID.packageFqName
//        log("LanguageGenerator.hasPackage $packageFqName -> $result")
//        return result
        // TODO()
        return false
    }

    object Key : GeneratedDeclarationKey()
//
//    private val companionPredicate = LookupPredicate.create {
//
//    }
//
//    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
//        register(companionPredicate)
//    }

//    override fun generateNestedClassLikeDeclaration(
//        owner: FirClassSymbol<*>,
//        name: Name,
//        context: NestedClassGenerationContext
//    ): FirClassLikeSymbol<*>? {
// //        return super.generateNestedClassLikeDeclaration(owner, name, context)
//        TODO()
//    }

    @OptIn(SymbolInternals::class)
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
//        return super.generateProperties(callableId, context)

        return if (callableId.callableName.identifier == "concept") {
            // PropertyBuildingContext(session, Key, callableId.classId!!, )

            val ps = FirPropertySymbol(callableId)

            val conceptClassId: ClassId = Concept::class.classId
            val conceptType: ConeKotlinType = conceptClassId.toConeType()
            val thisClass: FirClassSymbol<*> = callableId.classId!!.toSymbol(session) as FirClassSymbol<*>
            val property = createMemberProperty(thisClass, Key, Name.identifier("concept"), conceptType)

            ps.bind(property)

            // val fir =(callableId.classId!!.toSymbol(session)!! as FirClassSymbol<*>).pro
            // ps.bind(fir)
            listOf(ps)
        } else {
            emptyList()
        }
    }

//    override fun getCallableNamesForClass(
//        classSymbol: FirClassSymbol<*>,
//        context: MemberGenerationContext
//    ): Set<Name> {
//        val res = super.getCallableNamesForClass(classSymbol, context)
//        return res
//    }

//    override fun getNestedClassifiersNames(
//        classSymbol: FirClassSymbol<*>,
//        context: NestedClassGenerationContext
//    ): Set<Name> {
// //        return super.getNestedClassifiersNames(classSymbol, context)
//        // TODO we could define the concept here
//        return setOf(Name.identifier("Concept"))
//    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        val res = super.getTopLevelCallableIds()
        return res
    }

    @OptIn(SymbolInternals::class)
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? =
        runIf(name == DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            val firClass = createCompanionObject(owner, Key)
            firClass.symbol
        }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val constructor = createDefaultPrivateConstructor(context.owner, Key)
        return listOf(constructor.symbol)
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext,
    ): Set<Name> {
        if (!classSymbol.isCompanion) return emptySet()

        val origin = classSymbol.origin as? FirDeclarationOrigin.Plugin
        return runIf(origin?.key == Key) { setOf(SpecialNames.INIT, Name.identifier("concept")) }.orEmpty()
    }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
//        runIf(classSymbol matches companionPredicate and classSymbol.needsCompanion) {
//            setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
//        }.orEmpty()
        if (classSymbol.extendMPNode(session)) {
            println("FOR CLASS ${classSymbol.name} GOT COMPANION")
            return setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
        } else {
            return emptySet()
        }
    }

//    private infix fun FirClassSymbol<*>.matches(predicate: LookupPredicate): Boolean =
//        session.predicateBasedProvider.matches(predicate, this)
}

private val FirClassSymbol<*>.isCompanion get() =
    isSingleton &&
        with(classId) {
            isNestedClass && shortClassName == DEFAULT_NAME_FOR_COMPANION_OBJECT
        }

private val FirClassSymbol<*>.needsCompanion get() =
    !isSingleton && declarationSymbols.none { (it as? FirClassSymbol<*>)?.isCompanion ?: false }

private val FirClassSymbol<*>.isSingleton get() =
    classKind == ClassKind.OBJECT
