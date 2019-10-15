package com.strumenta.kolasu.generation

import com.github.salomonbrys.kotson.jsonObject
import com.github.salomonbrys.kotson.toJsonArray
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.model.processProperties
import com.strumenta.kolasu.validation.Error
import java.io.File

class JsonGenerator {

    fun generateJSON(root: Node): JsonElement {
        return root.toJson()
    }

    fun generateJSON(result: com.strumenta.kolasu.Result<out Node>): JsonElement {
        return jsonObject(
            "errors" to result.errors.map { it.toJson() },
            "root" to result.root?.toJson()
        )
    }

    fun generateString(root: Node): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(generateJSON(root))
    }

    fun generateString(result: com.strumenta.kolasu.Result<out Node>): String {
        val gson = GsonBuilder().setPrettyPrinting().create()
        return gson.toJson(generateJSON(result))
    }

    fun generateFile(root: Node, file: File) {
        File(file.toURI()).writeText(generateString(root))
    }

    fun generateFile(result: com.strumenta.kolasu.Result<out Node>, file: File) {
        File(file.toURI()).writeText(generateString(result))
    }
}

private fun Node.toJson(): JsonElement {
    val jsonObject = jsonObject(
        "type" to this.javaClass.simpleName,
        "position" to this.position?.toJson()
    )
    this.processProperties {
        if (it.value == null) {
            jsonObject.add(it.name, JsonNull.INSTANCE)
        } else if (it.multiple) {
            if (it.provideNodes) {
                jsonObject.add(it.name, (it.value as Collection<*>).map { (it as Node).toJson() }.toJsonArray())
            } else {
                jsonObject.add(it.name, (it.value as Collection<*>).toJsonArray())
            }
        } else {
            if (it.provideNodes) {
                jsonObject.add(it.name, (it.value as Node).toJson())
            } else {
                jsonObject.add(it.name, it.value.toJson())
            }
        }
    }
    return jsonObject
}

private fun Any?.toJson(): JsonElement? {
    return when (this) {
        null -> JsonNull.INSTANCE
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        else -> JsonPrimitive(this.toString())
    }
}

private fun Error.toJson(): JsonElement {
    return jsonObject(
        "type" to this.type.name,
        "message" to this.message,
        "position" to this.position?.toJson()
    )
}

private fun Position.toJson(): JsonElement {
    return jsonObject(
        "description" to this.toString(),
        "start" to this.start.toJson(),
        "end" to this.end.toJson()
    )
}

private fun Point.toJson(): JsonElement {
    return jsonObject(
        "line" to this.line,
        "column" to this.column
    )
}
