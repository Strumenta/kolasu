package com.strumenta.kolasu.interop

interface CodebaseAccess {
    val name: String

    /**
     * Return a list of LionWeb identifiers of the single files.
     */
    fun files() : Sequence<String>

    /**
     * Retrieve the file, in LionWeb format.
     */
    fun retrieveFile(fileIdentifier: String) : String
}