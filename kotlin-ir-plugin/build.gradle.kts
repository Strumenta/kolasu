

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("com.github.gmazzo.buildconfig")
    id("maven-publish")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    kapt("com.google.auto.service:auto-service:1.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.0.1")

    implementation(project(":core"))

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("com.github.tschuchortdev:kotlin-compile-testing:1.5.0")
}

buildConfig {
    packageName(group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlinPluginID"]}\"")
}

tasks {
    named("compileKotlin") {
        dependsOn("generateBuildConfig")
    }
    named("dokkaJavadoc") {
        dependsOn("kaptKotlin")
    }
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_ir_plugin", "Kotlin Compiler Plugin for Kolasu", project, false)
}
