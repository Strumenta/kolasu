package com.strumenta.kolasu.parserbench

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.saveAsJsonObject
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.parsing.Parser
import org.eclipse.emf.ecore.EPackage
import java.io.File
import java.io.FileWriter

class ParserBenchExampleGenerator(val parser: Parser<*>, val ePackage: EPackage, val directory: File) {

    fun generateMetamodel() {
        ePackage.saveAsJson(File(directory, "metamodel.json"))
    }

    fun generateExample(file: File) {
        generateExample(file.nameWithoutExtension, file.readText())
    }

    fun generateExample(name: String, file: File) {
        generateExample(name, file.readText())
    }

    fun generateExample(name: String, code: String) {
        val jo = JsonObject()
        jo.add("name", JsonPrimitive(name))
        jo.add("code", JsonPrimitive(code))

        val parsingResult = parser.parse(code)
        if (!parsingResult.isCorrect()) {
            throw IllegalStateException("Cannot generate examples from code with errors")
        }
        val astEMF = parsingResult.root!!.toEObject(ePackage)
        jo.add("ast", astEMF.saveAsJsonObject())
        jo.toString()
        val file = File(directory, "$name.json")
        val fw = FileWriter(file)
        fw.write(jo.toString())
        fw.close()
    }
}
