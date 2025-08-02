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
    "cliImplementation"(libs.clikt)
    testImplementation(kotlin("test-junit", libs.versions.kotlin.get()))
    testImplementation(libs.gson)
    implementation(libs.starlasu.specs)

    api(libs.lionweb.java)
    api(libs.lionweb.kotlin)

    api(project(":core"))
    implementation(libs.starlasu.specs)
}

mavenPublishing {
    coordinates(
        project.group.toString(),
        "starlasu-kotlin-${project.name}",
        project.version.toString(),
    )
}
