plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val lionwebVersion = extra["lionwebVersion"]
val kotlinVersion = extra["kotlinVersion"]
val cliktVersion = extra["clikt_version"]
val gsonVersion = extra["gson_version"] as String

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-core:$lionwebVersion")
    implementation("io.lionweb.lionweb-java:lionweb-java-2023.1-emf:$lionwebVersion")
    api(project(":core"))
    api(project(":emf"))
    api(project(":lionweb"))
    api(project(":ast"))

    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation("com.github.ajalt.clikt:clikt:$cliktVersion")
    implementation("com.google.code.gson:gson:$gsonVersion") {
        version {
            strictly(gsonVersion)
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
