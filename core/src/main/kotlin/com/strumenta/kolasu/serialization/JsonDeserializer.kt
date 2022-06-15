package com.strumenta.kolasu.serialization

import com.google.gson.*
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.IssueSeverity
import com.strumenta.kolasu.validation.IssueType
import com.strumenta.kolasu.validation.Result
import java.lang.IllegalStateException
import java.lang.UnsupportedOperationException
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

class JsonDeserializer {

    private val gsonBuilder = GsonBuilder()

    fun registerCustomDeserializer(type: KType, serializer: com.google.gson.JsonDeserializer<*>) {
        gsonBuilder.registerTypeAdapter(type.javaType, serializer)
    }

    private fun deserializeType(typeToDeserialize: KType, json: JsonElement?): Any? {
        if (json == null) {
            return null
        }
        if (typeToDeserialize.classifier is KClass<*>) {
            val rawClass: Class<*> = (typeToDeserialize.classifier as KClass<*>).java
            when {
                Node::class.java.isAssignableFrom(rawClass) -> {
                    val className = json.asJsonObject[JSON_TYPE_KEY].asString
                    val actualClass = Class.forName(className)
                    return deserialize(actualClass.asSubclass(Node::class.java), json.asJsonObject)
                }
                Collection::class.java.isAssignableFrom(rawClass) -> {
                    require(typeToDeserialize.arguments.size == 1)
                    val elementType = typeToDeserialize.arguments[0]
                    val ja = json.asJsonArray
                    val list = LinkedList<Any>()
                    for (jel in ja) {
                        list.add(deserializeType(elementType.type!!, jel)!!)
                    }
                    return list
                }
                // TODO handle enum classes
                rawClass == String::class.java -> {
                    return json.asString
                }
                rawClass == Int::class.java -> {
                    return json.asInt
                }
                else -> {
                    if (json.isJsonObject) {
                        if (json.asJsonObject.has(JSON_TYPE_KEY)) {
                            val type = json.asJsonObject.get(JSON_TYPE_KEY).asString
                            val clazz = Class.forName(type)
                            if (clazz == null) {
                                throw IllegalStateException("Unable to find class $type")
                            } else {
                                return deserializeType(clazz.kotlin.createType(), json)
                            }
                        }
                    }
                    return gsonBuilder.create().fromJson(json, typeToDeserialize.javaType)
                }
            }
        }
        TODO()
    }
    fun <T : Node> deserialize(clazz: Class<T>, json: String): T {
        val jo = JsonParser().parse(json).asJsonObject
        return deserialize(clazz, jo)
    }

    fun <T : Node> deserialize(clazz: Class<T>, jo: JsonObject): T {
        val instance: T?
        val primaryConstructor = clazz.kotlin.primaryConstructor
        if (primaryConstructor != null) {
            val args = HashMap<KParameter, Any?>()
            for (p in primaryConstructor.parameters) {
                try {
                    val value = deserializeType(p.type, jo.get(p.name))
                    args[p] = value
                } catch (t: Throwable) {
                    throw RuntimeException(
                        "Issue deserializing property ${p.name} of ${p.type}. JSON: ${jo.get(p.name)}", t
                    )
                }
            }
            instance = primaryConstructor.callBy(args)
        } else {
            if (clazz.kotlin.objectInstance != null) {
                return clazz.kotlin.objectInstance!!
            }
            val emptyConstructor = clazz.constructors.find { it.parameters.isEmpty() }
            if (emptyConstructor != null) {
                instance = clazz.getConstructor().newInstance()
            } else {
                throw IllegalStateException("Class ${clazz.canonicalName} has no primary or default constructor")
            }
        }
        if (instance != null && jo.has(JSON_POSITION_KEY)) {
            val position = jo[JSON_POSITION_KEY]?.asJsonObject?.decodeAsPosition()
            instance.position = position
        }

        return instance ?: throw UnsupportedOperationException()
    }

    fun <T : Node> deserializeResult(rootClass: Class<T>, json: String): Result<T> {
        val jo = JsonParser().parse(json).asJsonObject
        val errors = jo["issues"].asJsonArray.map { it.asJsonObject }.map {
            val type = IssueType.valueOf(it["type"].asString)
            val message = it["message"].asString
            val position = it["position"]?.asJsonObject?.decodeAsPosition()
            val severity = IssueSeverity.valueOf(it["severity"].asString)
            Issue(type, message, severity = severity, position = position)
        }
        val root = if (jo.has("root")) {
            deserialize(rootClass, jo["root"].asJsonObject)
        } else {
            null
        }

        return Result(errors, root)
    }

    fun <T : Node> deserializeParsingResult(rootClass: Class<T>, json: String): ParsingResult<T> {
        val jo = JsonParser().parse(json).asJsonObject
        val issues = jo["issues"].asJsonArray.map { it.asJsonObject }.map {
            val type = IssueType.valueOf(it["type"].asString)
            val message = it["message"].asString
            val position = it["position"]?.asJsonObject?.decodeAsPosition()
            Issue(type, message, position = position)
        }
        val root = if (jo.has("root")) {
            deserialize(rootClass, jo["root"].asJsonObject)
        } else {
            null
        }

        return ParsingResult(issues, root)
    }
}

private fun JsonObject.decodeAsPosition(): Position {
    val start = this["start"]!!.asJsonObject.decodeAsPoint()
    val end = this["end"]!!.asJsonObject.decodeAsPoint()
    return Position(start, end)
}

private fun JsonObject.decodeAsPoint(): Point {
    val line = this["line"]!!.asInt
    val column = this["column"]!!.asInt
    return Point(line, column)
}
