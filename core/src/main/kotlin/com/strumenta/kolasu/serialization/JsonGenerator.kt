package com.strumenta.kolasu.serialization

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.*
import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Issue
import com.strumenta.kolasu.validation.Result
import java.io.File
import java.util.function.Function
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

const val JSON_TYPE_KEY = "#type"
const val JSON_POSITION_KEY = "#position"

class JsonGenerator {

    var shortClassNames = false

    // DEPRECATED
    var jsonSerializer: JsonSerializer = AsStringJsonSerializer

    private val customSerializers: MutableMap<KType, com.google.gson.JsonSerializer<*>> = HashMap()
    private val gsonBuilder = GsonBuilder()

    fun registerCustomSerializer(type: KType, serializer: com.google.gson.JsonSerializer<*>) {
        customSerializers[type] = serializer
        gsonBuilder.registerTypeAdapter(type.javaType, serializer)
    }

    /**
     * Converts an AST to JSON format.
     */
    fun generateJSON(root: Node): JsonElement {
        return nodeToJson(root, shortClassNames)
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSON(result: Result<out Node>): JsonElement {
        return jsonObject(
            "errors" to result.issues.map { it.toJson() }.toJsonArray(),
            "root" to result.root?.let { nodeToJson(it, shortClassNames) }
        )
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSON(result: ParsingResult<out Node>): JsonElement {
        return jsonObject(
            "issues" to result.issues.map { it.toJson() }.toJsonArray(),
            "root" to result.root?.let { nodeToJson(it, shortClassNames) }
        )
    }

    /**
     * Converts "results" to JSON format.
     */
    fun generateJSONWithStreaming(result: Result<out Node>, writer: JsonWriter, shortClassNames: Boolean = false) {
        writer.beginObject()
        writer.name("errors")
        writer.beginArray()
        result.issues.forEach { it.toJsonStreaming(writer) }
        writer.endArray()
        writer.name("root")
        if (result.root == null) {
            writer.nullValue()
        } else {
            result.root.toJsonStreaming(writer, shortClassNames)
        }
        writer.endObject()
    }

    fun generateJSONWithStreaming(root: Node, writer: JsonWriter, shortClassNames: Boolean = false) {
        root.toJsonStreaming(writer, shortClassNames)
    }

    fun generateString(root: Node): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(generateJSON(root))
    }

    fun generateString(result: Result<out Node>): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(generateJSON(result))
    }

    fun generateString(result: ParsingResult<out Node>): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(generateJSON(result))
    }

    fun generateFile(root: Node, file: File) {
        File(file.toURI()).writeText(generateString(root))
    }

    fun generateFile(result: Result<out Node>, file: File) {
        File(file.toURI()).writeText(generateString(result))
    }

    fun generateFile(result: ParsingResult<out Node>, file: File) {
        File(file.toURI()).writeText(generateString(result))
    }

    private fun valueToJson(value: Any?): JsonElement {
        try {
            return when (value) {
                null -> JsonNull.INSTANCE
                is String -> JsonPrimitive(value)
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> {
                    return gsonBuilder.create().toJsonTree(value)
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to serialize $value (${value?.javaClass?.canonicalName})", e)
        }
    }

    private fun nodeToJson(
        node: Node,
        shortClassNames: Boolean = false
    ):
        JsonElement {
        val jsonObject = jsonObject(
            JSON_TYPE_KEY to if (shortClassNames) node.javaClass.simpleName else node.javaClass.canonicalName,
            JSON_POSITION_KEY to node.position?.toJson()
        )
        node.processProperties {
            try {
                if (it.value == null) {
                    jsonObject.add(it.name, JsonNull.INSTANCE)
                } else if (it.multiple) {
                    if (it.provideNodes) {
                        jsonObject.add(
                            it.name,
                            (it.value as Collection<*>).map { el ->
                                nodeToJson(
                                    el as Node,
                                    shortClassNames
                                )
                            }
                                .toJsonArray()
                        )
                    } else {
                        jsonObject.add(it.name, valueToJson(it.value))
                    }
                } else {
                    if (it.provideNodes) {
                        jsonObject.add(it.name, nodeToJson(it.value as Node, shortClassNames))
                    } else {
                        jsonObject.add(it.name, valueToJson(it.value))
                    }
                }
            } catch (e: Exception) {
                throw RuntimeException("Issue occurred while processing property $it of $node", e)
            }
        }
        return jsonObject
    }
}

private fun Node.toJsonStreaming(writer: JsonWriter, shortClassNames: Boolean = false) {
    writer.beginObject()
    writer.name(JSON_TYPE_KEY)
    writer.value(if (shortClassNames) this.javaClass.simpleName else this.javaClass.canonicalName)
    if (this.position != null) {
        writer.name(JSON_POSITION_KEY)
        this.position!!.toJsonStreaming(writer)
    }
    this.processProperties {
        writer.name(it.name)
        if (it.value == null) {
            writer.nullValue()
        } else if (it.multiple) {
            writer.beginArray()
            if (it.provideNodes) {
                (it.value as Collection<*>).forEach {
                    (it as Node).toJsonStreaming(writer, shortClassNames)
                }
            } else {
                (it.value as Collection<*>).forEach {
                    it.toJsonStreaming(writer)
                }
            }
            writer.endArray()
        } else {
            if (it.provideNodes) {
                (it.value as Node).toJsonStreaming(writer, shortClassNames)
            } else {
                it.value.toJsonStreaming(writer)
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
        else -> writer.value(this.toString())
    }
}

private fun Issue.toJson(): JsonElement {
    return jsonObject(
        "type" to this.type.name,
        "message" to this.message,
        "severity" to this.severity.name,
        "position" to this.position?.toJson()
    )
}

private fun Issue.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("type")
    writer.value(this.type.name)
    writer.name("message")
    writer.value(this.message)
    writer.name("position")
    if (this.position == null) {
        writer.nullValue()
    } else {
        this.position.toJsonStreaming(writer)
    }
    writer.endObject()
}

private fun Position.toJson(): JsonElement {
    return jsonObject(
        "description" to this.toString(),
        "start" to this.start.toJson(),
        "end" to this.end.toJson()
    )
}

private fun Position.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("description")
    writer.value(this.toString())
    writer.name("start")
    start.toJsonStreaming(writer)
    writer.name("end")
    end.toJsonStreaming(writer)
    writer.endObject()
}

private fun Point.toJson(): JsonElement {
    return jsonObject(
        "line" to this.line,
        "column" to this.column
    )
}

private fun Point.toJsonStreaming(writer: JsonWriter) {
    writer.beginObject()
    writer.name("line")
    writer.value(this.line)
    writer.name("column")
    writer.value(this.column)
    writer.endObject()
}
