plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

repositories {
    mavenCentral()
}

val kspVersion = extra["kspVersion"] as String
val kotlinVersion = extra["kotlinVersion"]

val completeKspVersion = if (kspVersion.contains("-")) kspVersion else "$kotlinVersion-$kspVersion"

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:$completeKspVersion")
    implementation(project(":core"))
}

val isReleaseVersion = !(version as String).endsWith("SNAPSHOT")

publishing {
    addSonatypeRepo(project)
    addPublication(
        "kolasu_lionweb_ksp",
        "Framework to work with AST and building languages. KSP plugin for LionWeb code generation",
        project
    )
}

signing {
    sign(publishing.publications["kolasu_lionweb_ksp"])
}
