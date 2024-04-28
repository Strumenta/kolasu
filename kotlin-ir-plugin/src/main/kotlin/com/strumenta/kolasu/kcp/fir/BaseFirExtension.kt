package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val COMPILER_PLUGIN_DEBUG = true

abstract class BaseFirExtension(
    session: FirSession,
) : FirDeclarationGenerationExtension(session) {
    protected fun log(text: String) {
        if (COMPILER_PLUGIN_DEBUG) {
            var file = File("compiler-plugin-log.txt")
            if (!file.exists()) {
                println("debug file in ${file.absolutePath}")
            }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val current = LocalDateTime.now().format(formatter)
            file.appendText("$current: $text\n")
        }
    }

    protected fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
    }
}

internal val FirClassSymbol<*>.isCompanion get() =
    isSingleton &&
        with(classId) {
            isNestedClass && shortClassName == DEFAULT_NAME_FOR_COMPANION_OBJECT
        }

internal val FirClassSymbol<*>.isSingleton get() =
    classKind == ClassKind.OBJECT
