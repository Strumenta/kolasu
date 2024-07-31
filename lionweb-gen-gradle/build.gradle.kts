plugins {
    id("java-gradle-plugin")
    kotlin("jvm")
    alias(libs.plugins.buildConfig)
    id("maven-publish")
    alias(libs.plugins.gradlePublish)
    id("org.jetbrains.dokka")
    `kotlin-dsl`
}

val kspVersion = extra["kspVersion"] as String
val kotlinVersion = extra["kotlinVersion"] as String
val lionwebGenGradlePluginID = extra["lionwebGenGradlePluginID"] as String
val completeKspVersion = if (kspVersion.contains("-")) kspVersion else "$kotlinVersion-$kspVersion"
val lionwebJavaVersion = libs.lionwebjava.get().version

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation(libs.lionwebjava)
    api(project(":lionweb-gen"))
    testImplementation(project(":lionweb-ksp"))
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    implementation(libs.gson)
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$completeKspVersion")
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

tasks.findByName("dokkaJavadoc")!!.dependsOn("generateBuildConfig")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":core:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":emf:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb:jar")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb-gen:jar")

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
