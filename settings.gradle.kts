pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "starlasu-kotlin"

include("core")
include("semantics")
include("javalib")
include("lionweb")
include("lionwebrepo-client")

include("lionweb-ksp")
include("lionweb-gen")
// include("lionweb-gen-gradle")
