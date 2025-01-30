plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm")
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.2.0"
    id("org.jetbrains.dokka")
    `kotlin-dsl`
}

val kspVersion = extra["kspVersion"] as String
val kotlin_version = extra["kotlin_version"] as String
val gson_version = extra["gson_version"] as String
val lwcoreGradlePluginID = extra["lwcoreGradlePluginID"] as String
val completeKspVersion = if (kspVersion.contains("-")) kspVersion else "${kotlin_version}-${kspVersion}"
val lionwebJavaVersion = libs.lionwebjava.get().version

dependencies {
    api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
    implementation(libs.lionwebjava)
    implementation(project(":lionweb"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

buildConfig {
    packageName("com.strumenta.kolasu.lionwebgen")
    buildConfigField("String", "PLUGIN_ID", "\"${lwcoreGradlePluginID}\"")
    buildConfigField("String", "PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
    buildConfigField("String", "LIONCORE_VERSION", "\"${lionwebJavaVersion}\"")
}

gradlePlugin {
    website.set("https://github.com/strumenta/kolasu")
    vcsUrl.set("https://github.com/strumenta/kolasu.git")
    plugins {
        create("lwcoreGradlePlugin") {
            id = lwcoreGradlePluginID as String
            displayName = "Kolasu LionWeb Gen"
            description = "Kolasu LionWeb Gen"
            tags.set(listOf("parsing", "ast", "starlasu", "lionweb"))
            implementationClass = "com.strumenta.starlasu2.LWCorePlugin"
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    named("dokkaJavadoc") {
        dependsOn(":lwcore:compileKotlin")
    }
}
