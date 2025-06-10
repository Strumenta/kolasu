import com.vanniktech.maven.publish.SonatypeHost

plugins {
    java
    `jvm-test-suite`
    kotlin("jvm")
    id("java-library")
    alias(libs.plugins.superPublish)
    alias(libs.plugins.buildConfig)
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
                implementation(libs.lionwebkotlinrepoclient)
                implementation(libs.kotlin.test.junit5)
                implementation("io.kotest:kotest-runner-junit5-jvm:5.8.0")
                implementation(libs.kotesttestcontainers)
                implementation("io.kotest:kotest-assertions-core:5.8.0")
                implementation("io.kotest:kotest-property:5.8.0")
                implementation("org.testcontainers:testcontainers:1.19.5")
                implementation("org.testcontainers:junit-jupiter:1.19.5")
                implementation("org.testcontainers:postgresql:1.19.5")
                implementation(libs.lionwebjavarepoclienttesting)
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
    implementation(libs.lionwebjava)
    implementation(project(":core"))
    implementation(project(":lionweb"))
    implementation(project(":semantics"))
    implementation(libs.lionwebkotlincore)
    implementation(libs.lionwebkotlinrepoclient)
    implementation(libs.starlasu.specs)

    testImplementation(kotlin("test-junit5"))
    testImplementation("commons-io:commons-io:2.7")
    testImplementation("org.slf4j:slf4j-simple:1.7.30")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.DEFAULT)
    signAllPublications()

    pom {
        name.set("kolasu-${project.name}")
        description.set("The Kotlin client for working with StarLasu ASTSs and the lionweb-repository")
        inceptionYear.set("2024")
        url.set("https://github.com/Strumenta/kolasu")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("ftomassetti")
                name.set("Federico Tomassetti")
                url.set("https://github.com/ftomassetti/")
            }
            developer {
                id.set("alessiostalla")
                name.set("Alessio Stalla")
                email.set("alessio.stalla@strumenta.com")
            }
            developer {
                id.set("lorenzoaddazi")
                name.set("Lorenzo Addazi")
                email.set("lorenzo.addazi@strumenta.com")
            }
        }
        scm {
            url.set("https://github.com/Strumenta/kolasu/")
            connection.set("scm:git:git://github.com/Strumenta/kolasu.git")
            developerConnection.set("scm:git:ssh://git@github.com/Strumenta/kolasu.git")
        }
    }
}

val jvmVersion = libs.versions.jvm.get()

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

afterEvaluate {
    tasks {
        named("generateMetadataFileForMavenPublication") {
            dependsOn("kotlinSourcesJar")
        }
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
