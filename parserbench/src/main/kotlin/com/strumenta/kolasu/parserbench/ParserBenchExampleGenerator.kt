package com.strumenta.kolasu.parserbench

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.strumenta.kolasu.emf.EMFEnabledParser
import com.strumenta.kolasu.emf.saveAsJsonObject
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.parsing.ParsingResult
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.common.util.URI
import org.emfjson.jackson.resource.JsonResourceFactory
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter

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
            throw ExampleGenerationFailure(
                parsingResult, "Cannot generate examples from code with errors")
        }

        val jo = JsonObject()
        jo.add("name", JsonPrimitive(name))
        jo.add("code", JsonPrimitive(code))
        val eObject = Result(parsingResult.issues, parsingResult.root).toEObject(resource)
        resource.contents.add(eObject)
        val ast: JsonObject?
        try {
            ast = eObject.saveAsJsonObject()
        } finally {
            resource.contents.remove(eObject)
        }
        jo.add("ast", ast)
        jo.addProperty("parsingTime", parsingResult.firstStage!!.time)
        jo.addProperty("astBuildingTime", parsingResult.time)
        jo.toString()
        val file = File(directory, "$name.json")
        val fw = FileWriter(file)
        fw.write(jo.toString())
        fw.close()
    }
}

class ExampleGenerationFailure(val result: ParsingResult<*>, message: String) : RuntimeException(message)