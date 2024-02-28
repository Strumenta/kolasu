package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildPrimaryConstructor
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class ConceptGenerator(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {
    companion object {
        val MY_CLASS_ID = ClassId(FqName.fromSegments(listOf("foo", "bar")), Name.identifier("MyClass"))
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        if (classId != MY_CLASS_ID) return null
        val klass =
            buildRegularClass {
                moduleData = session.moduleData
                origin = Key.origin
                status =
                    FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public,
                    )
                classKind = ClassKind.CLASS
                scopeProvider = session.kotlinScopeProvider
                name = classId.shortClassName
                symbol = FirRegularClassSymbol(classId)
                superTypeRefs.add(session.builtinTypes.anyType)
            }
        return klass.symbol
    }

    private fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
        val classId = context.owner.classId
        require(classId == MY_CLASS_ID)
        val constructor =
            buildPrimaryConstructor {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = Key.origin
                returnTypeRef =
                    buildResolvedTypeRef {
                        type = classId.toConeType()
                    }
                status =
                    FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public,
                    )
                symbol = FirConstructorSymbol(classId)
            }.also {
                it.containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(classId)
            }
        return listOf(constructor.symbol)
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        val function =
            buildSimpleFunction {
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                moduleData = session.moduleData
                origin = Key.origin
                status =
                    FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.FINAL,
                        EffectiveVisibility.Public,
                    )
                returnTypeRef = session.builtinTypes.stringType
                name = callableId.callableName
                symbol = FirNamedFunctionSymbol(callableId)
                // it's better to use default type on corresponding firClass to handle type parameters
                // but in this case we know that MyClass don't have any generics
                dispatchReceiverType = callableId.classId?.toConeType()
            }
        return listOf(function.symbol)
    }

    override fun getTopLevelClassIds(): Set<ClassId> = setOf(MY_CLASS_ID)

    override fun hasPackage(packageFqName: FqName): Boolean = packageFqName == MY_CLASS_ID.packageFqName

    object Key : GeneratedDeclarationKey()
}
