plugins {
    kotlin("jvm")
    kotlin("kapt")
    alias(libs.plugins.buildConfig)
    id("maven-publish")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    kapt("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")

    implementation(project(":core"))
    implementation(project(":ast"))

    testImplementation(kotlin("test-junit"))
    testImplementation("org.jetbrains.kotlin:kotlin-compiler-embeddable")
    testImplementation("dev.zacsweers.kctfork:core:0.5.0-alpha07")
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
    addPublication("kolasu_ir_plugin", "Kotlin Compiler Plugin for Kolasu", project, addPrefix = false)
}
