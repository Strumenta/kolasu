package com.strumenta.kolasu.parserbench

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.serialization.JsonGenerator

/**
 * A transpilation trace can be visualized to demonstrate how the transpiler work.
 */
class TranspilationTrace<S: Node, T: Node>(val originalCode: String,
                                           val sourceAST: S,
                                           val targetAST: T,
                                           val generatedCode: String)

fun <S: Node, T: Node>TranspilationTrace<S, T>.toJson(): JsonElement {
    val jo = JsonObject()
    jo.addProperty("originalCode", originalCode)
    jo.add("sourceAST", JsonGenerator().generateJSON(sourceAST))
    jo.add("targetAST", JsonGenerator().generateJSON(targetAST))
    jo.addProperty("generatedCode", generatedCode)
    return jo
}

fun <S: Node, T: Node>TranspilationTrace<S, T>.toJsonString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(toJson())
}
