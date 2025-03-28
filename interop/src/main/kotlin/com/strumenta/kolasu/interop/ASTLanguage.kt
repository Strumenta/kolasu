package com.strumenta.kolasu.interop

import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.SerializationProvider

object ASTLanguage {
    val language by lazy {
        val stream = this.javaClass.classLoader.getResourceAsStream("ast.language.json")
            ?: error("Resource not found: codebase.language.json")
        val text = stream.bufferedReader().use { it.readText() }
        val js = SerializationProvider.getStandardJsonSerialization(DEFAULT_LIONWEB_VERSION)
        val languages = js.deserializeToNodes(text).filterIsInstance<Language>()
        require(languages.size == 1)
        languages.first()
    }
}
