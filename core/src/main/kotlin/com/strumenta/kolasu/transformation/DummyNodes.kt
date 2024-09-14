package com.strumenta.kolasu.transformation

import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.PossiblyNamed
import com.strumenta.kolasu.model.ReferenceByName
import java.lang.reflect.ParameterizedType
import kotlin.random.Random
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

fun <T : Node> KClass<T>.dummyInstance(): T {
    val kClassToInstantiate = this.toInstantiableType()
    val emptyConstructor = kClassToInstantiate.constructors.find { it.parameters.isEmpty() }
    if (emptyConstructor != null) {
        return emptyConstructor.call()
    }
    val constructor = kClassToInstantiate.primaryConstructor!!
    val params = mutableMapOf<KParameter, Any?>()
    constructor.parameters.forEach { param ->
        val mt = param.type.javaType
        val value = when {
            param.type.isMarkedNullable -> null
            mt is ParameterizedType && mt.rawType == List::class.java -> mutableListOf<Any>()
            (param.type.classifier as KClass<*>).isSubclassOf(Node::class) ->
                (param.type.classifier as KClass<out Node>).dummyInstance()
            param.type == String::class.createType() -> "DUMMY"
            param.type.classifier == ReferenceByName::class -> ReferenceByName<PossiblyNamed>("UNKNOWN")
            param.type == Int::class.createType() -> 0
            param.type == Float::class.createType() -> 0.0f
            param.type == Long::class.createType() -> 0L
            param.type == Double::class.createType() -> 0.0
            param.type == Boolean::class.createType() -> false
            (param.type.classifier as KClass<*>).isSubclassOf(Enum::class)
            -> (param.type.classifier as KClass<*>).java.enumConstants[0]
            else -> TODO("Param type ${param.type}")
        }
        params[param] = value
    }
    return constructor.callBy(params)
}

private fun <T : Any> KClass<T>.toInstantiableType(): KClass<out T> {
    return when {
        this.isSealed -> {
            val subclasses = this.sealedSubclasses.filter { it.isDirectlyOrIndirectlyInstantiable() }
            if (subclasses.isEmpty()) {
                throw IllegalStateException("$this has no instantiable sealed subclasses")
            }
            val subClassWithEmptyParam = subclasses.find { it.constructors.any { it.parameters.isEmpty() } }
            if (subClassWithEmptyParam == null) {
                if (subclasses.size == 1) {
                    subclasses.first().toInstantiableType()
                } else {
                    // Some constructs are recursive (think of the ArrayType)
                    // We either find complex logic to find the ones that aren't or just pick one randomly.
                    // Eventually we will build a tree
                    val r = Random.Default
                    subclasses[r.nextInt(subclasses.size)].toInstantiableType()
                }
            } else {
                subClassWithEmptyParam.toInstantiableType()
            }
        }
        this.isAbstract -> {
            throw IllegalStateException("We cannot instantiate an abstract class (but we can handle sealed classes)")
        }
        this.java.isInterface -> {
            throw IllegalStateException("We cannot instantiate an interface")
        }
        else -> {
            this
        }
    }
}

fun <T : Any> KClass<T>.isDirectlyOrIndirectlyInstantiable(): Boolean {
    return if (this.isSealed) {
        this.sealedSubclasses.any { it.isDirectlyOrIndirectlyInstantiable() }
    } else {
        !this.isAbstract && !this.java.isInterface
    }
}
