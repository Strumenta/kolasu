package com.strumenta.kolasu.parserbench

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.model.children
import com.strumenta.kolasu.model.walk
import com.strumenta.kolasu.serialization.JsonGenerator
import java.util.IdentityHashMap

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
    val sourceIds = generateIds(sourceAST, "src-")
    val targetIds = generateIds(targetAST, "target-")
    val destinationIds = generateOriginIds(targetIds, targetAST)
    jo.add("sourceAST", JsonGenerator().generateJSON(sourceAST, withIds=sourceIds, withDestinationIds=destinationIds))
    jo.add("targetAST", JsonGenerator().generateJSON(targetAST, withIds=targetIds, withOriginIds=sourceIds))
    jo.addProperty("generatedCode", generatedCode)
    return jo
}

private fun generateOriginIds(destinationIds: IdentityHashMap<Node, String>, targetAST: Node):
        IdentityHashMap<Node, String> {
    val res = IdentityHashMap<Node, String>()
    targetAST.walk().forEach { targetNode ->
        if (targetNode.origin is Node) {
            res[targetNode.origin as Node] = destinationIds[targetNode]!!
        }
    }
    return res
}

private fun generateIds(ast: Node, prefix: String = ""): IdentityHashMap<Node, String> {
    var nextId = 1
    val res = IdentityHashMap<Node, String>()
    fun helper(node: Node) {
        res[node] = prefix + ((nextId++).toString())
        node.children.forEach { helper(it) }
    }
    helper(ast)
    return res
}

fun <S: Node, T: Node>TranspilationTrace<S, T>.toJsonString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(toJson())
}
