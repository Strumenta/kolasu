pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version "$kotlinVersion"
        id("org.jetbrains.kotlin.multiplatform") version "$kotlinVersion"
        id("org.jetbrains.kotlin.kapt") version "$kotlinVersion"
        id("org.jetbrains.dokka") version "$dokkaVersion"
    }
}

rootProject.name = "kolasu"
include("core")
include("antlr4j")
include("antlr4k")
include("emf")
include("playground")
include("javalib")
include("kotlin-ir-plugin")
include("kotlin-ir-plugin-gradle")
include("lionweb")
include("lionweb-gen")
include("lionweb-ksp")
include("lionweb-gen-gradle")
include("serialization")
include("cli")
include("ast")
include("semantics")
include("lionwebrepo-client")
