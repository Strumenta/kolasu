package com.strumenta.kolasu.kcp.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

const val COMPILER_PLUGIN_DEBUG = true

abstract class BaseFirExtension(session: FirSession,
) : FirDeclarationGenerationExtension(session)  {
    protected fun log(text: String) {
        if (COMPILER_PLUGIN_DEBUG) {
            val file = File("/Users/federico/repos/kolasu-mp-example/compiler-plugin-log.txt")
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val current = LocalDateTime.now().format(formatter)
            file.appendText("$current: $text\n")
        }
    }
}