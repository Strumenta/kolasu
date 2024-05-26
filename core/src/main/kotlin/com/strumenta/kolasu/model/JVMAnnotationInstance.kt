package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.StarLasuLanguage
import com.strumenta.kolasu.language.StarLasuLanguagesRegistry

sealed class JVMAnnotationInstance : AnnotationInstance() {
    @property:Internal
    override val annotation: Annotation by lazy {
        try {
            val languageAnnotation = this.javaClass.getAnnotation(LanguageAssociation::class.java)
            val language: StarLasuLanguage =
                if (languageAnnotation != null) {
                    languageAnnotation.language.objectInstance ?: throw IllegalStateException()
                } else {
                    StarLasuLanguagesRegistry.getLanguage(this.javaClass.kotlin.packageName)
                }
            language.getAnnotation(this.javaClass.simpleName)
        } catch (e: Exception) {
            throw RuntimeException("Issue while retrieving concept for class ${this.javaClass.canonicalName}", e)
        }
    }
}

abstract class JVMSingleAnnotationInstance : JVMAnnotationInstance()

abstract class JVMMultipleAnnotationInstance : JVMAnnotationInstance()
