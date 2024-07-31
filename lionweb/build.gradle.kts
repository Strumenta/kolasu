plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    implementation(libs.clikt)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation(libs.gson)
    testImplementation("io.github.mkfl3x:json-delta:1.3")
    api(libs.lionwebkotlincore) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit5")
    }
    api(libs.lionwebjava) {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-test-junit5")
    }
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
