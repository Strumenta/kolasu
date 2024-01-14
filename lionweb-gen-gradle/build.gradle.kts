plugins {
    id("java-gradle-plugin")
    id("org.jetbrains.kotlin.jvm")
    id("com.github.gmazzo.buildconfig") version "3.1.0"
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.2.0"
    id("org.jetbrains.dokka")
}

val kspVersion = extra["kspVersion"] as String
val kotlinVersion = extra["kotlinVersion"] as String
val lionwebVersion = extra["lionwebVersion"] as String
val gsonVersion = extra["gson_version"] as String
val completeKspVersion = if (kspVersion.contains("-")) kspVersion else "$kotlinVersion-$kspVersion"
val lionwebGenGradlePluginID = extra["lionwebGenGradlePluginID"] as String

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    api(project(":lionweb-gen"))
    testImplementation(project(":lionweb-ksp"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$completeKspVersion")
}

buildConfig {
    packageName("com.strumenta.kolasu.lionwebgen")
    buildConfigField("String", "PLUGIN_ID", "\"${lionwebGenGradlePluginID}\"")
    buildConfigField("String", "PLUGIN_GROUP", "\"${project.group}\"")
    buildConfigField("String", "PLUGIN_NAME", "\"${project.name}\"")
    buildConfigField("String", "PLUGIN_VERSION", "\"${project.version}\"")
    buildConfigField("String", "LIONCORE_VERSION", "\"${lionwebVersion}\"")
}

gradlePlugin {
    website.set("https://github.com/strumenta/kolasu")
    vcsUrl.set("https://github.com/strumenta/kolasu.git")
    plugins {
        create("lionwebGenGradlePlugin") {
            id = lionwebGenGradlePluginID
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

// test {
//    useJUnitPlatform()
// }

tasks.findByName("dokkaJavadoc")!!.dependsOn("generateBuildConfig")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":core:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":emf:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb:jar")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb-gen:jar")

afterEvaluate {
    tasks {
        named("generateMetadataFileForPluginMavenPublication") {
            dependsOn("kdocJar")
        }
    }
}
