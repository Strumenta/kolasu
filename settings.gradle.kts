pluginManagement {
    val kotlin_version: String by settings
    val dokka_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version "$kotlin_version"
        id("org.jetbrains.dokka") version "$dokka_version"
    }
}

rootProject.name = "kolasu"
include("core")
include("emf")
include("playground")
include("javalib")
include("antlr")
