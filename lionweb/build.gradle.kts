plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
    alias(libs.plugins.vanniktech.publish)
    id("signing")
    id("org.jetbrains.dokka")
}

java {
    registerFeature("cli") {
        usingSourceSet(sourceSets["main"])
    }
}

dependencies {
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    "cliImplementation"("com.github.ajalt.clikt:clikt:${libs.versions.clikt.get()}")
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
    testImplementation("com.google.code.gson:gson:${libs.versions.gson.get()}")
    implementation(libs.starlasu.specs)

    api(libs.lionweb.java)
    api(libs.lionweb.kotlin)

    api(project(":core"))
    implementation(libs.starlasu.specs)
}
