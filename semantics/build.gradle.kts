plugins {
    kotlin("jvm")
    alias(libs.plugins.vanniktech.publish)
    idea
    signing
    alias(libs.plugins.ktlint)
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    testImplementation(libs.kotlin.test)
}
