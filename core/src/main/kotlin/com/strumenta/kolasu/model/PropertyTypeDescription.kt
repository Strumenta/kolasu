package com.strumenta.kolasu.model

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf

data class PropertyTypeDescription(
    val name: String,
    val provideNodes: Boolean,
    val multiple: Boolean,
    val valueType: KType,
) {
    companion object {
        fun buildFor(property: KProperty1<*, *>): PropertyTypeDescription {
            val propertyType = property.returnType
            val classifier = propertyType.classifier as? KClass<*>
            val multiple = (classifier?.isSubclassOf(Collection::class) == true)
            val valueType: KType
            val provideNodes =
                if (multiple) {
                    valueType = propertyType.arguments[0].type!!
                    providesNodes(propertyType.arguments[0])
                } else {
                    valueType = propertyType
                    providesNodes(classifier)
                }
            return PropertyTypeDescription(
                name = property.name,
                provideNodes = provideNodes,
                multiple = multiple,
                valueType = valueType,
            )
        }
    }
}
