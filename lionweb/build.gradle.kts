plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val lionwebJavaVersion = extra["lionwebJavaVersion"]
val kotlinVersion = extra["kotlinVersion"]

dependencies {
    implementation(libs.clikt)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation(libs.gson)
    testImplementation("io.github.mkfl3x:json-delta:1.3")
    api("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebJavaVersion")
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
