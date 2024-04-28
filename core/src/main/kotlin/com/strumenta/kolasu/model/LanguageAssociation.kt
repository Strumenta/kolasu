package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class LanguageAssociation(
    val language: KClass<out StarLasuLanguage>,
)
