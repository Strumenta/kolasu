package com.strumenta.kolasu.parserbench

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.strumenta.kolasu.emf.saveAsJsonObject
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.parsing.Parser
import org.eclipse.emf.ecore.EPackage

class ParserBenchExampleGenerator(val parser : Parser<*>, val ePackage: EPackage) {

    fun generate(name: String, code: String) {
        val jo = JsonObject()
        jo.add("name", JsonPrimitive(name))
        jo.add("code", JsonPrimitive(code))

        val parsingResult = parser.parse(code)
        if (!parsingResult.isCorrect()) {
          throw IllegalStateException("Cannot generate examples from code with errors")
        }
        val astEMF = parsingResult.root!!.toEObject(ePackage)
        jo.add("ast", astEMF.saveAsJsonObject())
    }
}