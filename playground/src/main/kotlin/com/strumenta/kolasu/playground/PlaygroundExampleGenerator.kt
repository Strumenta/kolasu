package com.strumenta.kolasu.playground

import com.google.gson.JsonObject
import com.google.gson.internal.Streams
import com.google.gson.stream.JsonWriter
import com.strumenta.kolasu.emf.EcoreEnabledParser
import com.strumenta.kolasu.emf.createResource
import com.strumenta.kolasu.emf.saveAsJsonObject
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.NodeLike
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.Writer

class PlaygroundExampleGenerator<R : NodeLike>(
    val parser: EcoreEnabledParser<R, *, *, *>,
    val directory: File,
    val failOnError: Boolean = true,
    resourceURI: URI = URI.createURI(""),
) {
    val resource = createResource(resourceURI)!!

    fun generateMetamodel() {
        parser.generateMetamodel(resource)
        val metamodelFile = File(directory, "metamodel.json")
        FileOutputStream(metamodelFile).use {
            resource.save(it, null)
        }
    }

    fun generateExample(file: File) {
        generateExample(file.nameWithoutExtension, file.readText())
    }

    fun generateExample(
        name: String,
        file: File,
    ) {
        generateExample(name, file.readText())
    }

    fun generateExample(
        name: String,
        code: String,
    ) {
        val parsingResult = parser.parse(code)
        if (!parsingResult.isCorrect && failOnError) {
            throw ExampleGenerationFailure(parsingResult, "Cannot generate examples from code with errors")
        }

        val file = File(directory, "$name.json")
        FileWriter(file).use {
            parsingResult.saveForPlayground(resource, it, name)
        }
    }
}

class ExampleGenerationFailure(
    val result: ParsingResult<*>,
    message: String,
) : RuntimeException(message)

fun <N : NodeLike> ParsingResult<N>.saveForPlayground(
    resource: Resource,
    writer: Writer,
    name: String,
    indent: String = "",
) {
    val simplifiedResult: Result<N> = Result(issues, root)
    val eObject = simplifiedResult.toEObject(resource)
    try {
        resource.contents.add(eObject)
        val ast = eObject.saveAsJsonObject()
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", name)
        jsonObject.addProperty("code", code)
        jsonObject.add("ast", ast)
        if (time != null) {
            jsonObject.addProperty("parsingTime", time)
        }
        if (time != null) {
            jsonObject.addProperty("astBuildingTime", time)
        }
        val jsonWriter = JsonWriter(writer).apply { setIndent(indent) }
        jsonWriter.isLenient = true
        Streams.write(jsonObject, jsonWriter)
    } finally {
        resource.contents.remove(eObject)
    }
}
