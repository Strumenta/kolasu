package com.strumenta.kolasu.lionweb

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.strumenta.kolasu.emf.cli.KolasuLanguageGeneratorCommand
import com.strumenta.kolasu.language.KolasuLanguage
import io.lionweb.language.Language

class LanguageGenerator(kLanguage: KolasuLanguage, lwLanguage: Language? = null) : CliktCommand() {
    override fun run() = Unit

    init {
        if (lwLanguage == null) {
            subcommands(KolasuLanguageGeneratorCommand(kLanguage))
        } else {
            subcommands(KolasuLanguageGeneratorCommand(kLanguage), LionWebLanguageGeneratorCommand(lwLanguage))
        }
    }
}
