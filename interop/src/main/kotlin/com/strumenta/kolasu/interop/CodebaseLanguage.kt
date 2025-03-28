package com.strumenta.kolasu.interop

import io.lionweb.lioncore.java.language.Concept
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.SerializationProvider

object CodebaseLanguage {
    val language by lazy {
        val stream = this.javaClass.classLoader.getResourceAsStream("codebase.language.json")
            ?: error("Resource not found: codebase.language.json")
        val text = stream.bufferedReader().use { it.readText() }
        val js = SerializationProvider.getStandardJsonSerialization(DEFAULT_LIONWEB_VERSION)
        js.registerLanguage(ASTLanguage.language)
        js.instanceResolver.addTree(ASTLanguage.language)
        val languages = js.deserializeToNodes(text).filterIsInstance<Language>()
        require(languages.size == 1)
        languages.first()
    }
    val codebaseFile: Concept by lazy {
        language.getConceptByName("CodebaseFile") ?: throw IllegalStateException()
    }
    val builtinsCollection: Concept by lazy {
        language.getConceptByName("BuiltinsCollection") ?: throw IllegalStateException()
    }
}
