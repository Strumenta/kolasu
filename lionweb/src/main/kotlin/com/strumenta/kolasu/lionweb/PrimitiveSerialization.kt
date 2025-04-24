package com.strumenta.kolasu.lionweb

import com.strumenta.kolasu.model.Point
import com.strumenta.kolasu.model.Position
import com.strumenta.kolasu.parsing.KolasuToken
import com.strumenta.kolasu.parsing.TokenCategory
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveDeserializer
import io.lionweb.lioncore.java.serialization.PrimitiveValuesSerialization.PrimitiveSerializer
import io.lionweb.lioncore.kotlin.MetamodelRegistry

fun registerSerializersAndDeserializersInMetamodelRegistry() {
    MetamodelRegistry.addSerializerAndDeserializer(StarLasuLWLanguage.Char, charSerializer, charDeserializer)
    MetamodelRegistry.addSerializerAndDeserializer(StarLasuLWLanguage.Point, pointSerializer, pointDeserializer)
    MetamodelRegistry.addSerializerAndDeserializer(
        StarLasuLWLanguage.Position,
        positionSerializer,
        positionDeserializer
    )

    val tlpt = MetamodelRegistry.getPrimitiveType(TokensList::class, LIONWEB_VERSION_USED_BY_KOLASU)
        ?: throw IllegalStateException("Unknown primitive type class ${TokensList::class}")
    MetamodelRegistry.addSerializerAndDeserializer(tlpt, tokensListPrimitiveSerializer, tokensListPrimitiveDeserializer)
}

class TokensList(val tokens: List<KolasuToken>)

//
// Char
//

val charSerializer = PrimitiveSerializer<Char> { value -> "$value" }
val charDeserializer = PrimitiveDeserializer<Char> { serialized ->
    require(serialized.length == 1)
    serialized[0]
}

//
// Point
//

val pointSerializer: PrimitiveSerializer<Point> =
    PrimitiveSerializer<Point> { value ->
        if (value == null) {
            return@PrimitiveSerializer null
        }
        "L${value.line}:${value.column}"
    }

val pointDeserializer: PrimitiveDeserializer<Point> =
    PrimitiveDeserializer<Point> { serialized ->
        if (serialized == null) {
            return@PrimitiveDeserializer null
        }
        require(serialized.startsWith("L"))
        require(serialized.removePrefix("L").isNotEmpty())
        val parts = serialized.removePrefix("L").split(":")
        require(parts.size == 2)
        Point(parts[0].toInt(), parts[1].toInt())
    }

//
// Position
//

val positionSerializer = PrimitiveSerializer<Position> { value ->
    "${pointSerializer.serialize((value as Position).start)}-${pointSerializer.serialize(value.end)}"
}

val positionDeserializer = PrimitiveDeserializer<Position> { serialized ->
    if (serialized == null) {
        null
    } else {
        val parts = serialized.split("-")
        require(parts.size == 2) {
            "Position has an expected format: $serialized"
        }
        Position(pointDeserializer.deserialize(parts[0]), pointDeserializer.deserialize(parts[1]))
    }
}

//
// Tokens List
//

val tokensListPrimitiveSerializer = PrimitiveSerializer<TokensList?> { value: TokensList? ->
    value?.tokens?.joinToString(";") { kt ->
        kt.category.type + "$" + positionSerializer.serialize(kt.position)
    }
}

val tokensListPrimitiveDeserializer = PrimitiveDeserializer<TokensList?> { serialized ->
    if (serialized == null) {
        null
    } else {
        val tokens = if (serialized.isEmpty()) {
            mutableListOf()
        } else {
            serialized.split(";").map {
                val parts = it.split("$")
                require(parts.size == 2)
                val category = parts[0]
                val position = positionDeserializer.deserialize(parts[1])
                KolasuToken(TokenCategory(category), position)
            }.toMutableList()
        }
        TokensList(tokens)
    }
}
