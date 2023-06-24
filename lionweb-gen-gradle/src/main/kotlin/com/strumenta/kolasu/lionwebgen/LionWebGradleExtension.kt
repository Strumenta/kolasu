package com.strumenta.kolasu.lionwebgen

import org.gradle.api.provider.Property
import java.io.File

interface LionWebGradleExtension {
    val packageName: Property<String>
    val languages: Property<List<File>>
    val outdir: Property<File>
}