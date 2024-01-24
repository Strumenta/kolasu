package com.strumenta.kolasu.emf.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEPackage
import com.strumenta.kolasu.language.KolasuLanguage

class KolasuLanguageGeneratorCommand(val language: KolasuLanguage,
                                     private val kotlinPackageName: String = language.qualifiedName) : CliktCommand() {
    val languageFile by argument().file(canBeDir = false)

    override fun run() {
        language.toEPackage(kotlinPackageName=kotlinPackageName).saveAsJson(languageFile)
    }
}
