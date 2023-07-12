import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    id("org.jlleitschuh.gradle.ktlint") version "11.3.2"
    id("maven-publish")
    id("signing")

    id("java-library")
    id("net.researchgate.release") version "3.0.2"
    id("org.jetbrains.kotlinx.kover") version "0.7.1" apply(false)
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
    apply(plugin = "org.jetbrains.kotlinx.kover")

    this.version = rootProject.version
    this.group = rootProject.group

    val kotlinVersion = extra["kotlinVersion"]

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
        implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
        implementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")

        testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
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

    ktlint {
        filter {
            exclude { element ->
                element.file.absolutePath.split(File.separator).contains("build")
            }
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
