package com.strumenta.kolasu.language

import kotlin.Annotation
import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

actual fun simpleType(
    classifier: KClass<*>,
    nullable: Boolean,
): KType =
    object : KType {
        override val annotations: List<Annotation>
            get() = emptyList()
        override val arguments: List<KTypeProjection>
            get() = emptyList()
        override val classifier: KClassifier?
            get() = classifier
        override val isMarkedNullable: Boolean
            get() = nullable
    }
