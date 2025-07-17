import org.gradle.api.tasks.bundling.Jar
import org.gradle.plugins.signing.Sign
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.ktlint)
    alias(libs.plugins.vanniktech.publish)
    id("idea")
    id("signing")
    id("org.jetbrains.dokka") version libs.versions.dokka.get()
    id("java-library")
    alias(libs.plugins.release)
}

allprojects {
    group = "com.strumenta.kolasu"
    version = rootProject.version

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        }
    }
}

subprojects {

    tasks.withType<DokkaTask>().configureEach {
        dokkaSourceSets.named("main") {
            includeNonPublic.set(true)
            moduleName.set("kolasu-" + moduleName.get())
            includes.from("README.md")
        }
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn(":${project.name}:dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from(layout.buildDirectory.dir("dokka/javadoc"))
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvm.get()))
        }
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { isReleaseVersion }
    }

    plugins.withId("java") {
        the<JavaPluginExtension>().apply {
            toolchain {
                val version = libs.versions.jvm.get()
                val clean = if (version.startsWith("1.")) version.removePrefix("1.") else version
                languageVersion.set(JavaLanguageVersion.of(clean))
            }
            sourceCompatibility = JavaVersion.toVersion(version)
            targetCompatibility = JavaVersion.toVersion(version)
        }
    }
}

// Release logic
val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

configure<net.researchgate.release.ReleaseExtension> {
    buildTasks.set(listOf("publish"))
    git {
        requireBranch.set("")
        pushToRemote.set("origin")
    }
}

// Gradle wrapper configuration
tasks.named<Wrapper>("wrapper") {
    gradleVersion = "8.14.2"
    distributionType = Wrapper.DistributionType.ALL
}
