plugins {
    kotlin("jvm")
    alias(libs.plugins.ktlint)
    id("signing")
    id("org.jetbrains.dokka")
    alias(libs.plugins.vanniktech.publish)
    id("com.strumenta.starlasu.build.plugin")
}

val isReleaseVersion = !(project.version as String).endsWith("-SNAPSHOT")

dependencies {
    implementation(project(":core"))
    implementation(project(":lionweb"))
    implementation(kotlin("stdlib", libs.versions.kotlin.get()))
    implementation(kotlin("reflect", libs.versions.kotlin.get()))
    implementation(libs.starlasu.specs)
    implementation(libs.gson)

    testImplementation(kotlin("test", libs.versions.kotlin.get()))
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
}

tasks.named("dokkaJavadoc").configure {
    dependsOn(":core:compileKotlin")
    dependsOn(":lionweb:jar")
}
