package com.strumenta.kolasu.model

import java.io.File
import java.net.URL
import java.nio.file.Path

class SourceSet(
    val name: String,
    val root: Path,
)

class SourceSetElement(
    val sourceSet: SourceSet,
    val relativePath: Path,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:$relativePath"
}

data class FileSource(
    val file: File,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:${file.path}"
}

class URLSource(
    val url: URL,
) : Source() {
    override fun stringDescription(): String = "${this.javaClass.name}:$url"
}
