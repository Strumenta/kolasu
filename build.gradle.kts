import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint") version "11.3.2"
    id("maven-publish")
    id("idea")
    id("signing")

    id("java-library")
    id("net.researchgate.release") version "3.0.2"
}

allprojects {
    project.group = "com.strumenta.kolasu"
    project.version = version

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

val version = extra["kolasu_version"] as String
val isReleaseVersion = !version.endsWith("SNAPSHOT")

subprojects {

    tasks.withType(DokkaTask::class).configureEach {
        dokkaSourceSets {
            named("main") {
                includeNonPublic.set(true)
                moduleName.set("kolasu-" + moduleName.get())
                includes.from("README.md")
            }
        }
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn(":$name:dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        // See https://discuss.gradle.org/t/why-subproject-sourceset-dirs-project-sourceset-dirs/7376/5
        // Without the closure, parent sources are used for children too
        from(sourceSets.main)
    }

    tasks.withType(Test::class).all {
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

//    ktlint {
//        version = "0.48.1"
//        verbose = true
//        outputToConsole = true
//        enableExperimentalRules = true
//    }

    val jvm_version = extra["jvm_version"]!! as String

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
        kotlinOptions {
            jvmTarget = "$jvm_version"
        }
    }

    if (isReleaseVersion) {
        tasks.withType(Sign::class) {
        }
    }
}

release {
    buildTasks.set(listOf("publish"))
    git {
        requireBranch.set("master")
        pushToRemote.set("origin")
    }
}
