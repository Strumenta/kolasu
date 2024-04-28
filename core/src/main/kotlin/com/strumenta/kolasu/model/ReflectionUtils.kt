package com.strumenta.kolasu.model

import kotlin.reflect.KClass
import kotlin.reflect.KClassifier
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection

fun <T : NodeLike> T.relevantMemberProperties(
    withRange: Boolean = false,
    withNodeType: Boolean = false,
): List<KProperty1<T, *>> {
    val list = this::class.nodeProperties.map { it as KProperty1<T, *> }.toMutableList()
    if (withRange) {
        list.add(NodeLike::range as KProperty1<T, *>)
    }
    if (withNodeType) {
        list.add(NodeLike::nodeType as KProperty1<T, *>)
    }
    return list.toList()
}

private fun providesNodes(classifier: KClassifier?): Boolean {
    if (classifier == null) {
        return false
    }
    if (classifier is KClass<*>) {
        return providesNodes(classifier as? KClass<*>)
    } else {
        throw UnsupportedOperationException(
            "We are not able to determine if the classifier $classifier provides AST Nodes or not",
        )
    }
}

internal fun providesNodes(kTypeProjection: KTypeProjection): Boolean {
    val ktype = kTypeProjection.type
    return when (ktype) {
        is KClass<*> -> providesNodes(ktype as? KClass<*>)
        is KType -> providesNodes((ktype as? KType)?.classifier)
        else -> throw UnsupportedOperationException(
            "We are not able to determine if the type $ktype provides AST Nodes or not",
        )
    }
}
