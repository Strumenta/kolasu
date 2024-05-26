package com.strumenta.kolasu.model.dynamic

import com.strumenta.kolasu.language.Annotation
import com.strumenta.kolasu.language.Concept
import com.strumenta.kolasu.language.Feature
import com.strumenta.kolasu.model.AnnotationInstance
import com.strumenta.kolasu.model.ErrorNode
import com.strumenta.kolasu.model.Instantiator
import com.strumenta.kolasu.model.NodeLike

class DynamicInstantiator : Instantiator {
    override fun instantiateError(
        concept: Concept,
        message: String,
    ): ErrorNode {
        TODO("Not yet implemented")
    }

    override fun instantiate(
        concept: Concept,
        featureValues: Map<Feature, Any?>,
    ): NodeLike {
        TODO("Not yet implemented")
    }

    override fun instantiate(
        annotation: Annotation,
        featureValues: Map<Feature, Any?>,
    ): AnnotationInstance {
        val instance = DynamicAnnotationInstance(annotation)
        annotation.allFeatures.forEach { feature ->
            TODO()
        }
        return instance
    }
}
