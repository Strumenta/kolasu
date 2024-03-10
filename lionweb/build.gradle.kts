plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val lionwebVersion = extra["lionwebVersion"]
val kotlinVersion = extra["kotlinVersion"]
val cliktVersion = extra["clikt_version"]

dependencies {
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation(libs.gson)
    testImplementation("io.github.mkfl3x:json-delta:1.3")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    api(project(":core"))
    api(project(":ast"))

    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_lionweb", "Integration of Kolasu with LIonWeb", project)
}

signing {
    sign(publishing.publications["kolasu_lionweb"])
}
