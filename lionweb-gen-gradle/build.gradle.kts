plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.buildconfig)
    id("maven-publish")
    alias(libs.plugins.superPublish)
    id("org.jetbrains.dokka")
    `kotlin-dsl`
}

val lionwebGenGradlePluginID = extra["lionwebGenGradlePluginID"] as String
val lionwebJavaVersion = libs.lionwebjava.get().version

dependencies {
    api(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlin.reflect)
    implementation(libs.lionwebjava)

    api(project(":lionweb-gen"))
    testImplementation(project(":lionweb-ksp"))

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)

    implementation(libs.starlasu.specs)
    implementation(libs.gson)
    implementation(libs.symbol.processing.api)
}

buildConfig {
    packageName("com.strumenta.kolasu.lionwebgen")
    buildConfigField("String", "PLUGIN_ID", "\"${lionwebGenGradlePluginID}\"")
    buildConfigField("String", "PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
    buildConfigField("String", "LIONCORE_VERSION", "\"${lionwebJavaVersion}\"")
}

gradlePlugin {
    website.set("https://github.com/strumenta/kolasu")
    vcsUrl.set("https://github.com/strumenta/kolasu.git")
    plugins {
        create("lionwebGenGradlePlugin") {
            id = lionwebGenGradlePluginID as String
            displayName = "Kolasu LionWeb Gen"
            description = "Kolasu LionWeb Gen"
            tags.set(listOf("parsing", "ast", "starlasu", "lionweb"))
            implementationClass = "com.strumenta.kolasu.lionwebgen.LionWebGradlePlugin"
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateBuildConfig")
}

tasks.test {
    useJUnitPlatform()
}

tasks.findByName("dokkaJavadoc")!!.dependsOn("generateBuildConfig")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":core:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":emf:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb:jar")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb-gen:jar")

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
