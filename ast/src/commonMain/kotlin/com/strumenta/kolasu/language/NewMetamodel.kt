package com.strumenta.kolasu.language

import com.strumenta.kolasu.model.MPNode
import kotlin.reflect.KClass

class StarLasuLanguage {
    fun conceptForClass(kClass: KClass<*>): Concept {
        TODO()
    }

    fun instantiateNode(
        concept: Concept,
        featureValues: Map<Feature, Any?>,
    ): MPNode {
        TODO()
    }

    fun instantiateErrorNode(
        concept: Concept,
        message: String,
    ): MPNode {
        TODO()
    }
}

class Concept {
    val language: StarLasuLanguage
        get() = TODO()

    val name: String
        get() = TODO()

    fun instantiateNode(featureValues: Map<Feature, Any?>): MPNode {
        TODO()
    }

    fun instantiateErrorNode(message: String): MPNode {
        TODO()
    }
}
