package com.strumenta.kolasu.model

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Feature

interface Instantiator {
    fun instantiateError(
        concept: Concept,
        message: String,
    ): ErrorNode

    fun instantiate(
        concept: Concept,
        featureValues: Map<Feature, Any?>,
    ): NodeLike

    fun instantiate(
        annotation: Annotation,
        featureValues: Map<Feature, Any?>,
    ): AnnotationInstance
}
