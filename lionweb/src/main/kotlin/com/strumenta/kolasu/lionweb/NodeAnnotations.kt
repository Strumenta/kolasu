package com.strumenta.kolasu.lionweb

import io.lionweb.lioncore.java.model.AnnotationInstance
import java.util.IdentityHashMap

val KNode.annotations: MutableList<AnnotationInstance>
    get() {
        return nodeAnnotationsMap.computeIfAbsent(this) {
            mutableListOf()
        }
    }

private val nodeAnnotationsMap = IdentityHashMap<KNode, MutableList<AnnotationInstance>>()
