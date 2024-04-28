package com.strumenta.kolasu.language

object StarLasuLanguagesRegistry {
    fun getLanguage(name: String): StarLasuLanguage {
        if (!languages.containsKey(name)) {
            throw IllegalArgumentException("Language $name is unknown")
        }
        return languages[name]!!
    }

    fun registerLanguage(language: StarLasuLanguage) {
        languages[language.qualifiedName] = language
    }

    private val languages = mutableMapOf<String, StarLasuLanguage>()
}
