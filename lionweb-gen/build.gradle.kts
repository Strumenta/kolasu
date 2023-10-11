plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val lionwebVersion = extra["lionwebVersion"]
val kotlinVersion = extra["kotlinVersion"]
val clikt_version = extra["clikt_version"]
val gson_version = extra["gson_version"] as String

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.github.ajalt.clikt:clikt:$clikt_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    implementation("io.lionweb.lioncore-java:lioncore-java-core:$lionwebVersion")
    implementation("io.lionweb.lioncore-java:lioncore-java-emf:$lionwebVersion")
    api(project(":core"))
    api(project(":emf"))
    api(project(":lionweb"))

    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.github.ajalt.clikt:clikt:$clikt_version")
    implementation("com.google.code.gson:gson:$gson_version") {
        version {
            strictly(gson_version)
        }
    }

    // We use the embeddable version because it avoids issues with conflicting versions of dependencies
    // that we have when using the normal one
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

val isReleaseVersion = !(version as String).endsWith("SNAPSHOT")

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_lionweb_gen", "Integration of Kolasu with LIonWeb", project)
}
signing {
    sign(publishing.publications["kolasu_lionweb_gen"])
}

// configurations.all {
//    resolutionStrategy.eachDependency { details ->
//        if (details.requested.group == "com.google.code.gson") {
//            details.useVersion(gson_version)
//        }
//    }
// }

tasks.findByName("dokkaJavadoc")!!.dependsOn(":core:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":emf:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb:jar")
