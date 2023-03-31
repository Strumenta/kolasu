package com.strumenta.kolasu.emf

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import java.io.File

@Deprecated("Deprecating everything EMF related")
fun EPackage.saveEcore(ecoreFile: File, restoringURI: Boolean = true) {
    val startURI = this.eResource().uri
    val resource = createResource(URI.createFileURI(ecoreFile.absolutePath))!!
    resource.contents.add(this)
    resource.save(null)
    if (restoringURI) {
        this.setResourceURI(startURI.toString())
    }
}
