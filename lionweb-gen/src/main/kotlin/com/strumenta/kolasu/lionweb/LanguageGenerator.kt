package com.strumenta.kolasu.lionweb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization

class LanguageGeneratorCommand(val language: Language) : CliktCommand() {
    val languageFile by argument().file(canBeDir = false)

    override fun run() {
        JsonSerialization.saveLanguageToFile(language, languageFile)
    }
}