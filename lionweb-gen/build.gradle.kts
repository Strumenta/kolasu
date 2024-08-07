plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("org.jetbrains.dokka")
}

val kotlinVersion = extra["kotlinVersion"]

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    implementation(libs.clikt)
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    implementation(libs.lionwebjava)
    implementation(libs.lionwebjavaemf)
    api(project(":core"))
    api(project(":emf"))
    api(project(":lionweb"))
    api(project(":ast"))

    implementation("com.squareup:kotlinpoet:1.14.2")
    implementation(libs.gson) {
        version {
            strictly(libs.gson.get().version!!)
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

tasks.findByName("dokkaJavadoc")!!.dependsOn(":core:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":emf:compileKotlin")
tasks.findByName("dokkaJavadoc")!!.dependsOn(":lionweb:jar")
