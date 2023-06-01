import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    id("com.github.gmazzo.buildconfig")
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.2.0"
}

dependencies {
    implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
    val project = project(":kotlin-ir-plugin")
    packageName(project.group.toString())
    buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlinPluginID"]}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")
}

gradlePlugin {
    plugins {
        create("kotlinIrPluginTemplate") {
            id = rootProject.extra["kotlinPluginID"] as String
            displayName = "Kotlin Ir Plugin Template"
            description = "Kotlin Ir Plugin Template"
            implementationClass = "com.strumenta.kolasu.kcp.gradle.StarLasuGradlePlugin"
        }
    }
}

publishing {
    addSonatypeRepo(project)
}

afterEvaluate {
    tasks {
        named("generateMetadataFileForPluginMavenPublication") {
        dependsOn("kdocJar")
    }
    }
}

tasks {

    named("compileKotlin") {
        dependsOn("generateBuildConfig")
    }

//    named("generateMetadataFileForPluginMavenPublication") {
//        dependsOn("kdocJar")
//    }
}
