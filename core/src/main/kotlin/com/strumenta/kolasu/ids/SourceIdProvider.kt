package com.strumenta.kolasu.ids

import com.strumenta.kolasu.model.FileSource
import com.strumenta.kolasu.model.Source
import com.strumenta.kolasu.model.SyntheticSource
import java.io.File

/**
 * Given a Source (even null), it generates a corresponding identifier.
 */
interface SourceIdProvider {
    fun sourceId(source: Source?): String
}

abstract class AbstractSourceIdProvider : SourceIdProvider {
    protected fun cleanId(id: String) = id.replace('.', '-').replace('/', '-')
}

class SimpleSourceIdProvider : AbstractSourceIdProvider() {
    override fun sourceId(source: Source?): String {
        return when (source) {
            null -> {
                "UNKNOWN_SOURCE"
            }
            is FileSource -> {
                cleanId("file_${source.file.path}")
            }
            is SyntheticSource -> {
                cleanId("synthetic_${source.description}")
            }
            else -> {
                TODO("Unable to generate ID for Source $this (${this.javaClass.canonicalName})")
            }
        }
    }
}

class RelativeSourceIdProvider(val baseDir: File, var rootName: String? = null) : AbstractSourceIdProvider() {
    override fun sourceId(source: Source?): String {
        return when (source) {
            null -> {
                "UNKNOWN_SOURCE"
            }
            is FileSource -> {
                val thisAbsPath = source.file.absolutePath
                val baseAbsPath = baseDir.absolutePath
                val expectedPrefix = baseAbsPath + File.separator
                require(thisAbsPath.startsWith(expectedPrefix))
                val relativePath = thisAbsPath.removePrefix(expectedPrefix)
                val id = if (rootName == null) relativePath else "${rootName}___$relativePath"
                return cleanId(id)
            }
            else -> {
                TODO("Unable to generate ID for Source $this (${this.javaClass.canonicalName})")
            }
        }
    }
}
