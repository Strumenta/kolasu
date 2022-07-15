package com.strumenta.kolasu.emf

import org.eclipse.emf.common.util.URI
import org.eclipse.emf.ecore.EPackage
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl
import java.io.File

fun EPackage.saveEcore(ecoreFile: File, restoringURI: Boolean = true) {
    val startURI = this.eResource().uri
    val resource = createResource(URI.createFileURI(ecoreFile.absolutePath))!!
    resource.contents.add(this)
    resource.save(null)
    if (restoringURI) {
        this.setResourceURI(startURI.toString())
    }
}
