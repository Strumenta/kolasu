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

// val version = extra["kolasu_version"] as String
val isReleaseVersion = !(version as String).endsWith("SNAPSHOT")

subprojects {

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")
    apply(plugin = "idea")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")

    this.version = rootProject.version
    this.group = rootProject.group

    val kotlin_version = extra["kotlin_version"]

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlin_version")
        implementation("org.jetbrains.kotlin:kotlin-test:$kotlin_version")

        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
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

tasks.wrapper {
    gradleVersion = "8.1.1"
}
