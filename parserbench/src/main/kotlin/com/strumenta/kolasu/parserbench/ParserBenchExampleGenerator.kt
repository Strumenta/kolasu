package com.strumenta.kolasu.parserbench

import com.google.gson.JsonObject
import com.strumenta.kolasu.emf.EMFEnabledParser
import com.strumenta.kolasu.emf.saveAsJsonObject
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emfcloud.jackson.resource.JsonResourceFactory
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.Writer

class ParserBenchExampleGenerator(
    val parser: EMFEnabledParser<*, *, *>,
    val directory: File,
    val failOnError: Boolean = true,
    resourceURI: URI = URI.createURI("")
) {

    val resource = JsonResourceFactory().createResource(resourceURI)

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

    fun generateExample(name: String, file: File) {
        generateExample(name, file.readText())
    }

    fun generateExample(name: String, code: String) {
        val parsingResult = parser.parse(code)
        if (!parsingResult.correct && failOnError) {
            throw ExampleGenerationFailure(parsingResult, "Cannot generate examples from code with errors")
        }

        val file = File(directory, "$name.json")
        FileWriter(file).use {
            parsingResult.saveForParserBench(resource, it, name)
        }
    }
}

class ExampleGenerationFailure(val result: ParsingResult<*>, message: String) : RuntimeException(message)

fun ParsingResult<*>.saveForParserBench(
    metamodel: Resource,
    writer: Writer,
    name: String
) {
    val simplifiedResult = Result(issues, root)
    val eObject = simplifiedResult.toEObject(metamodel)
    try {
        metamodel.contents.add(eObject)
        val ast = eObject.saveAsJsonObject()
        val jsonObject = JsonObject()
        jsonObject.addProperty("name", name)
        jsonObject.addProperty("code", code)
        jsonObject.add("ast", ast)
        if (firstStage != null) {
            jsonObject.addProperty("parsingTime", firstStage!!.time)
        }
        jsonObject.addProperty("astBuildingTime", time)
        writer.write(jsonObject.toString())
    } finally {
        metamodel.contents.remove(eObject)
    }
}
