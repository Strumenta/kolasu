package com.strumenta.kolasu.lionweb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import io.lionweb.language.Language
import io.lionweb.serialization.JsonSerialization

class LionWebLanguageGeneratorCommand(val language: Language) : CliktCommand() {
    val languageFile by argument().file(canBeDir = false)

    override fun run() {
        JsonSerialization.saveLanguageToFile(language, languageFile)
    }
}
