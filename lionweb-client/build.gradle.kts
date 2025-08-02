plugins {
    java
    `jvm-test-suite`
    kotlin("jvm")
    id("java-library")
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.build.config)
    
}

repositories {
    mavenLocal()
    mavenCentral()
}

testing {
    suites {
        val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }

        register<JvmTestSuite>("functionalTest") {
            dependencies {
                implementation(project())
                implementation(project(":core"))
                implementation(project(":lionweb"))
                implementation(project(":semantics"))
                implementation(libs.lionweb.kotlin.client)
                implementation(libs.kotlin.test.junit5)
                implementation(libs.kotest.runner.junit5)
                implementation(libs.testcontainers)
                implementation(libs.kotest.assertions.core)
                implementation(libs.kotest.property)
                implementation(libs.testcontainers.junit5)
                implementation(libs.testcontainers.pg)
                implementation(libs.lionweb.java.client.testing)
            }

            targets {
                all {
                    testTask.configure {
                        shouldRunAfter(test)
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(libs.lionweb.java)
    implementation(project(":core"))
    implementation(project(":lionweb"))
    implementation(project(":semantics"))
    implementation(libs.lionweb.kotlin)
    implementation(libs.lionweb.kotlin.client)
    implementation(libs.starlasu.specs)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.commons.io)
    testImplementation(libs.slf4j)
}

val jvmVersion = libs.versions.jvm.get()

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

val lionwebRepositoryCommitID = extra["lionwebRepositoryCommitID"]

buildConfig {
    sourceSets.getByName("functionalTest") {
        packageName("com.strumenta.kolasu.lionwebclient")
        buildConfigField("String", "LIONWEB_REPOSITORY_COMMIT_ID", "\"${lionwebRepositoryCommitID}\"")
        useKotlinOutput()
    }
}
