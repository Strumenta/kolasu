package com.strumenta.kolasu.lionwebgen

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import java.io.File

interface LionWebGradleExtension {
    val importPackageNames: MapProperty<String, String>
    val languages: ListProperty<File>
    val outdir: Property<File>
    val exportPackages: ListProperty<String>
}
