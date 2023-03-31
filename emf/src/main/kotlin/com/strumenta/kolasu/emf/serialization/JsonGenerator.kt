package com.strumenta.kolasu.emf.serialization

import com.strumenta.kolasu.emf.saveAsJson
import com.strumenta.kolasu.emf.toEObject
import com.strumenta.kolasu.model.ASTNode
import com.strumenta.kolasu.validation.Result
import org.eclipse.emf.ecore.EPackage

@Deprecated("Deprecating everything EMF related")
class JsonGenerator {
    fun generateEMFString(result: Result<out ASTNode>, astPackage: EPackage): String {
        return result.toEObject(astPackage).saveAsJson()
    }
}
