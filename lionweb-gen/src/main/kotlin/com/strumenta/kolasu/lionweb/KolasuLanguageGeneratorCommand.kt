package com.strumenta.kolasu.lionweb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEPackage
import com.strumenta.kolasu.language.KolasuLanguage
import io.lionweb.lioncore.java.language.Language
import io.lionweb.lioncore.java.serialization.JsonSerialization

class KolasuLanguageGeneratorCommand(val language: KolasuLanguage) : CliktCommand() {
    val languageFile by argument().file(canBeDir = false)

    override fun run() {
        language.toEPackage().saveAsJson(languageFile)
    }
}
