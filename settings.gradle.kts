pluginManagement {
    val kotlin_version: String by settings
    val dokka_version: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version "$kotlin_version"
        id("org.jetbrains.dokka") version "$dokka_version"
        id("com.github.gmazzo.buildconfig") version "3.1.0"
        id("org.jetbrains.kotlinx.kover") version "0.7.1"
    }
}

rootProject.name = "kolasu"
include("core")
include("antlr")
include("emf")
include("playground")
include("javalib")
include("kotlin-ir-plugin")
include("kotlin-ir-plugin-gradle")
include("lionweb")
include("lionweb-gen")
include("lionweb-ksp")
include("lionweb-gen-gradle")