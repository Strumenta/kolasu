package com.strumenta.kolasu.generation

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.*
import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import java.io.File
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class JsonDeserializer {

    private fun deserializeType(type: KType, json: JsonElement?): Any? {
        if (json == null) {
            return null
        }
        if (type.classifier is KClass<*>) {
            val rawClass : Class<*> = (type.classifier as KClass<*>).java
            when {
                Node::class.java.isAssignableFrom(rawClass) -> {
                    val className = json.asJsonObject["type"].asString
                    val actualClass = Class.forName(className)
                    return deserialize(actualClass as Class<out Node>, json.asJsonObject)
                }
                Collection::class.java.isAssignableFrom(rawClass) -> {
                    require(type.arguments.size == 1)
                    val elementType = type.arguments[0]
                    val ja = json.asJsonArray
                    val list = LinkedList<Any>()
                    for (jel in ja) {
                        list.add(deserializeType(elementType.type!!, jel)!!)
                    }
                    return list
                }
                rawClass == String::class.java -> {
                    return json.asString
                }
                rawClass == Int::class.java -> {
                    return json.asInt
                }
                else -> {
                    TODO()
                }
            }
        }
        TODO()
    }
    fun <T:Node> deserialize(clazz: Class<T>, json: String): T {
        val jo = JsonParser().parse(json).asJsonObject
        return deserialize(clazz, jo)
    }

    fun <T:Node> deserialize(clazz: Class<T>, jo: JsonObject): T {
        var instance : T? = null
        val primaryConstructor = clazz.kotlin.primaryConstructor
        if (primaryConstructor != null) {
            var args = HashMap<KParameter, Any?>()
            for (p in primaryConstructor.parameters) {
                val value = deserializeType(p.type, jo.get(p.name))
                args[p] = value
            }
            instance = primaryConstructor!!.callBy(args)
        } else {
            val emptyConstructor = clazz.constructors.find { it.parameters.isEmpty() }
            if (emptyConstructor != null) {
                instance = clazz.newInstance()
            } else {
                throw IllegalStateException("Class ${clazz.canonicalName} has no primary or default constructor")
            }
        }

        return instance ?: throw UnsupportedOperationException()
    }
}
