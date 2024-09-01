package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.kcp.classId
import com.strumenta.kolasu.kcp.fir.MPNodesCollector.knownMPNodeSubclasses
import com.strumenta.kolasu.language.Concept
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberProperty
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.ir.backend.js.utils.TODO
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.runIf

// @OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)
class LanguageGenerator(
    session: FirSession,
) : BaseFirExtension(session) {
    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        TODO()
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        return emptyList()
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelClassIds(): Set<ClassId> {
        return emptySet()
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        return false
    }

    object Key : GeneratedDeclarationKey()

    @OptIn(
        org
            .jetbrains
            .kotlin
            .fir
            .FirImplementationDetail::class,
    )
    override fun generateProperties(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirPropertySymbol> {
        return if (callableId.callableName.identifier == "concept") {
            val ps = FirPropertySymbol(callableId)

            val conceptClassId: ClassId = Concept::class.classId
            val conceptType: ConeKotlinType = conceptClassId.toConeType()
            val thisClass: FirClassSymbol<*> = callableId.classId!!.toSymbol(session) as FirClassSymbol<*>
            val property = createMemberProperty(thisClass, Key, Name.identifier("concept"), conceptType)

            ps.bind(property)

            listOf(ps)
        } else {
            emptyList()
        }
    }

    @ExperimentalTopLevelDeclarationsGenerationApi
    override fun getTopLevelCallableIds(): Set<CallableId> {
        return super.getTopLevelCallableIds()
    }

    @OptIn(SymbolInternals::class)
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? =
        runIf(name == DEFAULT_NAME_FOR_COMPANION_OBJECT) {
            if (owner.fir.declarations.filterIsInstance<FirClassLikeDeclaration>().none {
                    it.nameOrSpecialName == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
                }
            ) {
                val firClass = createCompanionObject(owner, Key)
                firClass.symbol
            } else {
                null
            }
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
        if (classSymbol.extendMPNode(session)) {
            knownMPNodeSubclasses.add(classSymbol.name)
            println("FOR CLASS ${classSymbol.name} GOT COMPANION")
            return setOf(DEFAULT_NAME_FOR_COMPANION_OBJECT)
        } else {
            return emptySet()
        }
    }
}

object MPNodesCollector {
    val knownMPNodeSubclasses = mutableSetOf<Name>()
}
