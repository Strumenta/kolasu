package com.strumenta.kolasu.serialization

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.language.Containment
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Range
import com.strumenta.kolasu.model.ReferenceValue
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.traversing.walk
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import java.io.File
import java.util.IdentityHashMap
import java.util.UUID
import java.util.function.Function
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

const val JSON_TYPE_KEY = "#type"
const val JSON_RANGE_KEY = "#range"
const val JSON_ORIGIN_KEY = "#origin"
const val JSON_ID_KEY = "#id"
const val JSON_DESTINATION_KEY = "#destination"

fun Iterable<*>.toJsonArray() = jsonArray(this.iterator())

fun jsonArray(values: Iterator<Any?>): JsonArray {
    val array = JsonArray()
    for (value in values)
        array.add(value.toJsonElement())
    return array
}

private fun Any?.toJsonElement(): JsonElement {
    if (this == null) {
        return JsonNull.INSTANCE
    }

    return when (this) {
        is JsonElement -> this
        is String -> toJson()
        is Number -> toJson()
        is Char -> toJson()
        is Boolean -> toJson()
        else -> throw IllegalArgumentException("$this cannot be converted to JSON")
    }
}

fun jsonObject(vararg values: Pair<String, *>): JsonObject {
    val jo = JsonObject()
    values.forEach {
        jo.add(it.first, it.second.toJsonElement())
    }
    return jo
}

/**
 * Converts an AST to JSON.
 * Note that ASTs may also be exported to the EMF-JSON format, which is different.
 */
class JsonGenerator {
    var shortClassNames = false

    private val customSerializers: MutableMap<KType, com.google.gson.JsonSerializer<*>> = HashMap()
    private val gsonBuilder = GsonBuilder()

    fun registerCustomSerializer(
        type: KType,
        serializer: com.google.gson.JsonSerializer<*>,
    ) {
        customSerializers[type] = serializer
        gsonBuilder.registerTypeAdapter(type.javaType, serializer)
    }

    /**
     * Converts an AST to JSON format.
     */
    fun generateJSON(
        root: NodeLike,
        withIds: IdentityHashMap<NodeLike, String>? = null,
        withOriginIds: IdentityHashMap<NodeLike, String>? = null,
        withDestinationIds: IdentityHashMap<NodeLike, String>? = null,
        shortClassNames: Boolean = false,
    ): JsonElement {
        return nodeToJson(
            root,
            shortClassNames,
            withIds = withIds,
            withOriginIds = withOriginIds,
            withDestinationIds = withDestinationIds,
        )
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSON(
        result: Result<out NodeLike>,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): JsonElement {
        return jsonObject(
            "issues" to result.issues.map { it.toJson() }.toJsonArray(),
            "root" to result.root?.let { nodeToJson(it, shortClassNames, withIds) },
        )
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSON(
        result: ParsingResult<out NodeLike>,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): JsonElement {
        return jsonObject(
            "issues" to result.issues.map { it.toJson() }.toJsonArray(),
            "root" to result.root?.let { nodeToJson(it, shortClassNames, withIds) },
        )
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSONWithStreaming(
        result: Result<out NodeLike>,
        writer: JsonWriter,
        shortClassNames: Boolean = false,
    ) {
        writer.beginObject()
        writer.name("issues")
        writer.beginArray()
        result.issues.forEach { it.toJsonStreaming(writer) }
        writer.endArray()
        if (result.root == null) {
            // do nothing for consistency with non-streaming JSON generation
        } else {
            writer.name("root")
            generateJSONWithStreaming(result.root!!, writer, shortClassNames)
        }
        writer.endObject()
    }

    fun generateJSONWithStreaming(
        root: NodeLike,
        writer: JsonWriter,
        shortClassNames: Boolean = false,
    ) {
        val gson = gsonBuilder.create()
        gson.toJson(
            generateJSON(
                root = root,
                withIds = null,
                withOriginIds = null,
                withDestinationIds = null,
                shortClassNames = shortClassNames,
            ),
            writer,
        )
    }

    fun generateString(
        root: NodeLike,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): String {
        val gson = gsonBuilder.setPrettyPrinting().create()
        return gson.toJson(generateJSON(root, withIds))
    }

    fun generateString(
        result: Result<out NodeLike>,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): String {
        val gson = gsonBuilder.setPrettyPrinting().create()
        return gson.toJson(generateJSON(result, withIds))
    }

    fun generateString(
        result: ParsingResult<out NodeLike>,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): String {
        val gson = gsonBuilder.setPrettyPrinting().create()
        return gson.toJson(generateJSON(result, withIds))
    }

    fun generateFile(
        root: NodeLike,
        file: File,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ) {
        File(file.toURI()).writeText(generateString(root, withIds))
    }

    fun generateFile(
        result: Result<out NodeLike>,
        file: File,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ) {
        File(file.toURI()).writeText(generateString(result, withIds))
    }

    fun generateFile(
        result: ParsingResult<out NodeLike>,
        file: File,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ) {
        File(file.toURI()).writeText(generateString(result, withIds))
    }

    private fun valueToJson(
        value: Any?,
        withIds: IdentityHashMap<NodeLike, String>? = null,
    ): JsonElement {
        try {
            return when (value) {
                null -> JsonNull.INSTANCE
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                is ReferenceValue<*> -> {
                    val jsonObject = JsonObject()
                    jsonObject.addProperty("name", value.name)
                    if (withIds != null) {
                        jsonObject.addProperty(
                            "referred",
                            if (value.isResolved) withIds[value.referred as NodeLike] ?: "<unknown>" else null,
                        )
                    }
                    jsonObject
                }

                else -> {
                    return gsonBuilder.create().toJsonTree(value)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to serialize $value (${value?.javaClass?.canonicalName})", e)
        }
    }

    private fun computeIds(root: NodeLike): IdentityHashMap<NodeLike, String> =
        IdentityHashMap<NodeLike, String>().apply {
            root.walk().forEach { this[it] = UUID.randomUUID().toString() }
        }

    private fun computeIds(result: Result<out NodeLike>): IdentityHashMap<NodeLike, String> =
        if (result.root != null) computeIds(result.root!!) else IdentityHashMap()

    private fun computeIds(result: ParsingResult<out NodeLike>): IdentityHashMap<NodeLike, String> =
        if (result.root != null) computeIds(result.root!!) else IdentityHashMap()

    private fun nodeToJson(
        node: NodeLike,
        shortClassNames: Boolean = false,
        withIds: IdentityHashMap<NodeLike, String>? = null,
        withOriginIds: IdentityHashMap<NodeLike, String>? = null,
        withDestinationIds: IdentityHashMap<NodeLike, String>? = null,
    ): JsonElement {
        val nodeType = if (shortClassNames) node.nodeType else node.qualifiedNodeType
        val jsonObject =
            jsonObject(
                JSON_TYPE_KEY to nodeType,
                JSON_RANGE_KEY to node.range?.toJson(),
            )
        if (withIds != null) {
            val id = withIds[node]
            if (id != null) {
                jsonObject.addProperty(JSON_ID_KEY, id)
            }
        }
        if (withOriginIds != null) {
            if (node.origin is NodeLike) {
                jsonObject.addProperty(JSON_ORIGIN_KEY, withOriginIds[node.origin as NodeLike] ?: "<unknown>")
            }
        }
        if (withDestinationIds != null) {
            val destinationId = withDestinationIds[node]
            if (destinationId != null) {
                jsonObject.addProperty(JSON_DESTINATION_KEY, destinationId)
            }
        }
        node.concept.allFeatures.filter { !it.derived }.forEach {
            try {
                if (it.value(node) == null) {
                    jsonObject.add(it.name, JsonNull.INSTANCE)
                } else if (it.isMultiple) {
                    if (it is Containment) {
                        jsonObject.add(
                            it.name,
                            (it.value(node) as Collection<*>)
                                .map { el ->
                                    nodeToJson(
                                        el as NodeLike,
                                        shortClassNames,
                                        withIds = withIds,
                                        withOriginIds = withOriginIds,
                                        withDestinationIds = withDestinationIds,
                                    )
                                }.toJsonArray(),
                        )
                    } else {
                        jsonObject.add(it.name, valueToJson(it.value(node), withIds))
                    }
                } else {
                    if (it is Containment) {
                        jsonObject.add(
                            it.name,
                            nodeToJson(
                                it.value(node) as NodeLike,
                                shortClassNames,
                                withIds = withIds,
                                withOriginIds = withOriginIds,
                                withDestinationIds = withDestinationIds,
                            ),
                        )
                    } else {
                        jsonObject.add(it.name, valueToJson(it.value(node), withIds))
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Issue occurred while processing property $it of $node", e)
            }
        }
        return jsonObject
    }
}

private fun NodeLike.toJsonStreaming(
    writer: JsonWriter,
    shortClassNames: Boolean = false,
) {
    writer.beginObject()
    writer.name(JSON_TYPE_KEY)
    writer.value(if (shortClassNames) this.javaClass.simpleName else this.javaClass.canonicalName)
    if (this.range != null) {
        writer.name(JSON_RANGE_KEY)
        this.range!!.toJsonStreaming(writer)
    }
    this.concept.declaredFeatures.filter { !it.derived }.forEach {
        writer.name(it.name)
        if (it.value(this) == null) {
            writer.nullValue()
        } else if (it.isMultiple) {
            writer.beginArray()
            if (it is Containment) {
                (it.value(this) as Collection<*>).forEach {
                    (it as NodeLike).toJsonStreaming(writer, shortClassNames)
                }
            } else {
                (it.value(this) as Collection<*>).forEach {
                    it.toJsonStreaming(writer)
                }
            }
            writer.endArray()
        } else {
            if (it is Containment) {
                (it.value(this) as NodeLike).toJsonStreaming(writer, shortClassNames)
            } else {
                it.value(this).toJsonStreaming(writer)
            }
        }
    }
    writer.endObject()
}

// DEPRECATED use GSon stuff instead
typealias JsonSerializer = Function<Any, JsonElement>

// DEPRECATED use GSon stuff instead
private val AsStringJsonSerializer = JsonSerializer { JsonPrimitive(it.toString()) }

private fun Any?.toJson(jsonSerializer: JsonSerializer = AsStringJsonSerializer): JsonElement {
    try {
        return when (this) {
            null -> JsonNull.INSTANCE
            is String -> JsonPrimitive(this)
            is Number -> JsonPrimitive(this)
            is Boolean -> JsonPrimitive(this)
            else -> jsonSerializer.apply(this)
        }
    } catch (e: Exception) {
        throw RuntimeException("Unable to serialize $this (${this?.javaClass?.canonicalName})", e)
    }
}

private fun Any?.toJsonStreaming(writer: JsonWriter) {
    when (this) {
        null -> writer.nullValue()
        is String -> writer.value(this)
        is Number -> writer.value(this)
        is Boolean -> writer.value(this)
        is Range -> this.toJsonStreaming(writer)
        else -> writer.value(this.toString())
    }
}

fun Issue.toJson(): JsonElement =
    jsonObject(
        "type" to this.type.name,
        "message" to this.message,
        "severity" to this.severity.name,
        "range" to this.range?.toJson(),
    )

private fun Issue.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("type")
    writer.value(this.type.name)
    writer.name("message")
    writer.value(this.message)
    writer.name("range")
    if (this.range == null) {
        writer.nullValue()
    } else {
        this.range.toJsonStreaming(writer)
    }

    writer.name("severity")
    writer.value(this.severity.name)
    writer.endObject()
}

fun Range.toJson(): JsonElement =
    jsonObject(
        "description" to this.toString(),
        "start" to this.start.toJson(),
        "end" to this.end.toJson(),
    )

private fun Range.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("description")
    writer.value(this.toString())
    writer.name("start")
    start.toJsonStreaming(writer)
    writer.name("end")
    end.toJsonStreaming(writer)
    writer.endObject()
}

private fun Point.toJson(): JsonElement =
    jsonObject(
        "line" to this.line,
        "column" to this.column,
    )

private fun Point.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("line")
    writer.value(this.line)
    writer.name("column")
    writer.value(this.column)
    writer.endObject()
}
