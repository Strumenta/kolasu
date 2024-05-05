package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.kcp.fir.LanguageGenerator.Key
import com.strumenta.kolasu.kcp.fir.MPNodesCollector.knownMPNodeSubclasses
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.builder.FirRegularClassBuilder
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.utils.addToStdlib.runIf


class ErrorClassGenerator(
    session: FirSession,
) : BaseFirExtension(session) {

    val errorClassName = Name.identifier("Error")

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        return super.generateConstructors(context)
    }

    @OptIn(SymbolInternals::class)
    override fun generateNestedClassLikeDeclaration(
        owner: FirClassSymbol<*>,
        name: Name,
        context: NestedClassGenerationContext,
    ): FirClassLikeSymbol<*>? =
        runIf(name == errorClassName) {
            val firClass : FirClass =FirRegularClassBuilder()
                .apply {
                    val classId = owner.classId.createNestedClassId(errorClassName)
                    this.name = errorClassName
                    this.origin = FirDeclarationOrigin.Plugin(Key)
                    this.moduleData = owner.fir.moduleData
                    this.status = FirDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL
                    )
                    this.classKind = ClassKind.CLASS
                    this.scopeProvider = FirKotlinScopeProvider()
                    this.symbol = FirRegularClassSymbol(classId)
                }
                .build()
            firClass.symbol
        }

    override fun getNestedClassifiersNames(
        classSymbol: FirClassSymbol<*>,
        context: NestedClassGenerationContext,
    ): Set<Name> {
        if (classSymbol.extendMPNode(session)) {
            knownMPNodeSubclasses.add(classSymbol.name)
            println("FOR CLASS ${classSymbol.name} GOT ERROR")
            return setOf(errorClassName)
        } else {
            return emptySet()
        }
    }

    object Key : GeneratedDeclarationKey()
}

