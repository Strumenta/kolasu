package com.strumenta.kolasu.lionwebgen

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

interface LionWebGradleExtension {
    val packageName: Property<String>
    val languages: ListProperty<File>
    val outdir: Property<File>
    val exportPackages: ListProperty<String>
}