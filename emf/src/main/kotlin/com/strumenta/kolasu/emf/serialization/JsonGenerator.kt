package com.strumenta.kolasu.emf.serialization

import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.Node
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.ecore.EPackage

class JsonGenerator {
    fun generateEMFString(result: Result<out Node>, astPackage: EPackage): String {
        return result.toEObject(astPackage).saveAsJson()
    }
}
