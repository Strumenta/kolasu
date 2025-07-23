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
                implementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
                implementation(libs.kotest.testcontainers)
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("org.testcontainers:testcontainers:1.19.5")
                implementation("org.testcontainers:junit-jupiter:1.19.5")
                implementation("org.testcontainers:postgresql:1.19.5")
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
    testImplementation("commons-io:commons-io:2.14.0")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
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
