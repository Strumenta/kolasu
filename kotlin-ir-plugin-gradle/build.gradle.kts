plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    alias(libs.plugins.buildConfig)
    id("maven-publish")
    alias(libs.plugins.gradlePublish)
    `kotlin-dsl`
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
    website.set("https://github.com/strumenta/kolasu")
    vcsUrl.set("https://github.com/strumenta/kolasu.git")
    plugins {
        create("kotlinIrPluginTemplate") {
            id = rootProject.extra["kotlinPluginID"] as String
            displayName = "Kolasu Multiplatform"
            description = "Kolasu Multiplatform"
            implementationClass = "com.strumenta.kolasu.kcp.gradle.StarLasuGradlePlugin"
        }
    }
}

publishing {
    addSonatypeRepo(project)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
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
