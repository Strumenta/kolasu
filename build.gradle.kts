import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    alias(libs.plugins.ktlint)
    id("maven-publish")
    id("signing")

    id("java-library")
    id("net.researchgate.release") version "3.0.2"
    alias(libs.plugins.kover) apply(false)
}

allprojects {
    project.group = "com.strumenta.kolasu"
    project.version = version

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val isReleaseVersion = !(version as String).endsWith("-SNAPSHOT")

subprojects {

    if (this.name != "ast" && this.name != "antlr4k") {
        apply(plugin = "org.jetbrains.kotlin.jvm")

        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "maven-publish")
        apply(plugin = "idea")
        apply(plugin = "signing")
        apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "org.jetbrains.kotlinx.kover")

        this.version = rootProject.version
        this.group = rootProject.group

        val kotlinVersion = extra["kotlinVersion"]

        if (this.name != "lionwebrepo-client") {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
                implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

                testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
            }
        }

        java {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        tasks.withType(DokkaTask::class).configureEach {
            dokkaSourceSets {
                named("main") {
                    includeNonPublic.set(true)
                    moduleName.set("kolasu-" + moduleName.get())
                }
            }
        }

        tasks.register<Jar>("kdocJar") {
            dependsOn("dokkaJavadoc")
            from((tasks.named("dokkaJavadoc").get() as DokkaTask).outputDirectory)
            archiveClassifier.set("javadoc")
        }

        tasks.register<Jar>("sourcesJar") {
            archiveClassifier.set("sources")
            // See https://discuss.gradle.org/t/why-subproject-sourceset-dirs-project-sourceset-dirs/7376/5
            // Without the closure, parent sources are used for children too
            from(sourceSets["main"].allSource)
        }

        tasks.named("publish") {
            dependsOn("kdocJar")
            dependsOn("sourcesJar")
        }

        tasks.named("publishToMavenLocal") {
            dependsOn("kdocJar")
            dependsOn("sourcesJar")
        }

        tasks.withType(Test::class).all {
            testLogging {
                showStandardStreams = true
                showExceptions = true
                exceptionFormat =
                    org
                        .gradle
                        .api
                        .tasks
                        .testing
                        .logging
                        .TestExceptionFormat
                        .FULL
            }
        }

        val jvmVersion = extra["jvm_version"]!! as String

        tasks
            .withType(
                KotlinCompile::class,
            ).all {
                kotlinOptions {
                    jvmTarget = "$jvmVersion"
                }
            }

        tasks.withType(Sign::class) {
            enabled = isReleaseVersion
        }

        ktlint {
            filter {
                exclude { element ->
                    element
                        .file
                        .absolutePath
                        .split(File.separator)
                        .contains("build")
                }
            }
        }

        tasks
            .withType<KotlinCompile>()
            .configureEach {
                compilerOptions
                    .languageVersion
                    .set(
                        KOTLIN_1_9,
                    )
            }
    }
}

release {
    buildTasks.set(listOf("publish", ":lionweb-gen-gradle:publishPlugins", ":kotlin-ir-plugin-gradle:publishPlugins"))
    git {
        requireBranch.set("")
        pushToRemote.set("origin")
    }
}

tasks.wrapper {
    gradleVersion = "8.9"
    distributionType = Wrapper.DistributionType.ALL
}
