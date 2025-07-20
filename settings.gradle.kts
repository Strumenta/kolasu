pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
    }
    includeBuild("build-logic/maven-publish-plugin")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.7.0"
}

rootProject.name = "starlasu-kotlin"

include("core")
include("codebase")
include("semantics")
include("javalib")
include("lionweb")
include("lionweb-client")
