//import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI

plugins {
    `jvm-test-suite`
    id("org.jetbrains.kotlin.jvm")
    //alias(libs.plugins.kotlinJvm)
    //alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
    id("java-library")
    alias(libs.plugins.superPublish)
}

repositories {
    mavenLocal()
    mavenCentral()
}

tasks.withType<Test>().all {
    testLogging {
        showStandardStreams = true
        showExceptions = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val jvmVersion = extra["jvm_version"] as String

dependencies {
    api(libs.lionwebjava)
    api(libs.lionwebkotlincore)
//    api(libs.lionwebkotlincore) {
//        exclude group: "org.jetbrains.kotlin", module: "kotlin-test-junit5"
//    }
}

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
        languageVersion.set(JavaLanguageVersion.of(jvmVersion.removePrefix("1.")))
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
