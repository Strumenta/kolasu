package com.strumenta.kolasu.lionwebgen

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.tasks.SourceSetContainer

fun Project.propertyValue(
    name: String,
    defaultValue: String? = null,
): String =
    if (project.extra.has(name)) {
        project.extra[name] as String
    } else {
        defaultValue ?: throw GradleException("Property $name is required")
    }

val Project.extra: ExtraPropertiesExtension
    get() = (this as ExtensionAware).extensions.getByName("ext") as ExtraPropertiesExtension

val Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

val Project.kolasuVersion
    get() = project.propertyValue("kolasuVersion", BuildConfig.PLUGIN_VERSION)

val Project.lionwebJavaVersion
    get() = project.propertyValue("lionwebJavaVersion", BuildConfig.LIONCORE_VERSION)
