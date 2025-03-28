package com.strumenta.kolasu.interop

import io.lionweb.lioncore.java.model.Node as LWNode

interface CodebaseAccess {
    val name: String

    /**
     * Return a list of LionWeb identifiers of the single files.
     */
    fun files(): Sequence<String>

    /**
     * Retrieve the file, in LionWeb format.
     */
    fun retrieveFile(fileIdentifier: String): LWNode
}
