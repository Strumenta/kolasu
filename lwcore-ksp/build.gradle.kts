plugins {
    kotlin("jvm") version "1.9.0"
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" // Use the latest KSP plugin version
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":lwcore"))
    implementation(kotlin("stdlib"))
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.0-1.0.13") // KSP API
}

// Ensure generated code is recognized
sourceSets.main {
    java.srcDir("build/generated/ksp/main/kotlin")
}

ksp {
    arg("ksp.verbose", "true")
}

val jvmVersion = extra["jvm_version"] as String
val normalizedJvmVersion = if (jvmVersion.startsWith("1.")) jvmVersion.removePrefix("1.") else jvmVersion

java {
    sourceCompatibility = JavaVersion.toVersion(jvmVersion)
    targetCompatibility = JavaVersion.toVersion(jvmVersion)
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = jvmVersion
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(normalizedJvmVersion.toInt()))
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(normalizedJvmVersion.toInt()))
    }
}


tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<Test> {
    testLogging {
        events("standardOut", "passed", "skipped", "failed")
    }
}