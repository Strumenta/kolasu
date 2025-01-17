package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.StarLasuLanguage
import kotlin.reflect.KClass

/**
 * This ensures that the generation of the Concept and other facilities is performed for the annotated
 * node.
 */
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class KolasuGen(
    val language: KClass<out StarLasuLanguage> = PlaceholderLanguage::class,
)

object PlaceholderLanguage : StarLasuLanguage("dummy")
