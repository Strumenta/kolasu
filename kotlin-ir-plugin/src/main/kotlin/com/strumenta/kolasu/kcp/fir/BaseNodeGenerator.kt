package com.strumenta.kolasu.kcp.fir

import com.strumenta.kolasu.model.BaseNode
import com.strumenta.kolasu.model.FeatureDescription
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.impl.FirImplicitAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val GENERATED_CALCULATE_FEATURES = "calculateFeatures"
const val GENERATED_CALCULATE_NODE_TYPE = "calculateNodeType"

const val COMPILER_PLUGIN_DEBUG = true

class BaseNodeGenerator(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {
    private fun log(text: String) {
        if (COMPILER_PLUGIN_DEBUG) {
            val file = File("/Users/federico/repos/kolasu-mp-example/compiler-plugin-log.txt")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val current = LocalDateTime.now().format(formatter)
            file.appendText("$current: $text\n")
        }
    }

    override fun generateTopLevelClassLikeDeclaration(classId: ClassId): FirClassLikeSymbol<*>? {
        log("generateTopLevelClassLikeDeclaration $classId")
        return generateTopLevelClassLikeDeclaration(classId)
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
        if (callableId.callableName.identifier == GENERATED_CALCULATE_FEATURES) {
            val name = Name.identifier(GENERATED_CALCULATE_FEATURES)
            val listClassId = ClassId.fromString(List::class.qualifiedName!!.replace(".", "/"))
            val featureDescriptionClassId =
                ClassId.fromString(
                    FeatureDescription::class.qualifiedName!!.replace(".", "/"),
                )
            val type: ConeKotlinType =
                listClassId.createConeType(
                    session,
                    typeArguments = arrayOf(featureDescriptionClassId.createConeType(session)),
                )
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
        if (callableId.callableName.identifier == GENERATED_CALCULATE_NODE_TYPE) {
            val name = Name.identifier(GENERATED_CALCULATE_NODE_TYPE)
            val type: ConeKotlinType = session.builtinTypes.stringType.type
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
        if (classSymbol.extendBaseNode(session) && !classSymbol.isAbstract && !classSymbol.isSealed) {
            log("  ${classSymbol.classId.asSingleFqName().asString()} extends BaseNode")
            val set =
                mutableSetOf(
                    Name.identifier(GENERATED_CALCULATE_FEATURES),
                    Name.identifier(GENERATED_CALCULATE_NODE_TYPE),
                )
            return set
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
fun FirClassSymbol<*>.isOrExtendBaseNode(firSession: FirSession): Boolean =
    isBaseNode(firSession) || extendBaseNode(firSession)

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.isBaseNode(firSession: FirSession): Boolean =
    this.classId.asSingleFqName().asString() == BaseNode::class.qualifiedName!!

@OptIn(SymbolInternals::class)
fun FirClassSymbol<*>.extendBaseNode(firSession: FirSession): Boolean =
    this.fir.superTypeRefs.any {
        when (it) {
            is FirResolvedTypeRefImpl -> {
                (it.type.classId!!.toSymbol(firSession) as FirClassSymbol<*>).isOrExtendBaseNode(firSession)
            }
            is FirImplicitAnyTypeRef -> {
                false
            }

            else -> {
                throw IllegalStateException("Processing ${it.javaClass.canonicalName} ($it)")
            }
        }
    }
