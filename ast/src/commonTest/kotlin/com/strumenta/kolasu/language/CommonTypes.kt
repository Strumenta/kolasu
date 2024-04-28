package com.strumenta.kolasu.language

import kotlin.reflect.KClass
import kotlin.reflect.KType

expect fun simpleType(
    classifier: KClass<*>,
    nullable: Boolean = false,
): KType
