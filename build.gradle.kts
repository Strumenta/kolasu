import org.jetbrains.dokka.gradle.DokkaTask
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
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
        maven(url="https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven(url="https://central.sonatype.com/repository/maven-snapshots/")
    }

}
subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java-library")
    apply(plugin = "signing")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    tasks.withType<DokkaTask>().configureEach {
        dokkaSourceSets.named("main") {
            includeNonPublic.set(true)
            moduleName.set("kolasu-${project.name}")
            includes.from("README.md")
        }
    }

    tasks.register<Jar>("javadocJar") {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from("$buildDir/dokka/javadoc")
    }

    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(project.the<SourceSetContainer>()["main"].allSource)
    }

    tasks.withType<Test>().configureEach {
        testLogging {
            showStandardStreams = true
            showExceptions = true
            exceptionFormat = TestExceptionFormat.FULL
        }
    }

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("0.48.1")
        verbose.set(true)
        outputToConsole.set(true)
        enableExperimentalRules.set(true)
        disabledRules.set(
                listOf("no-wildcard-imports", "experimental:argument-list-wrapping")
        )
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = project.property("jvm_version").toString()
    }

    tasks.withType<Sign>().configureEach {
        onlyIf { !project.version.toString().endsWith("SNAPSHOT") }
    }

    java {
        val cleanVersion = project.property("jvm_version").toString().removePrefix("1.")
        toolchain.languageVersion.set(JavaLanguageVersion.of(cleanVersion))
        sourceCompatibility = JavaVersion.toVersion(project.property("jvm_version").toString())
        targetCompatibility = JavaVersion.toVersion(project.property("jvm_version").toString())
        registerFeature("cli") {
            usingSourceSet(sourceSets["main"])
        }
    }

    kotlin {
        val cleanVersion = project.property("jvm_version").toString().removePrefix("1.")
        jvmToolchain(cleanVersion.toInt())
    }
}

val isReleaseVersion = !version.toString().endsWith("SNAPSHOT")

configure<net.researchgate.release.ReleaseExtension> {
    buildTasks = listOf("publish", ":lionweb-gen-gradle:publishPlugins")
    git.apply {
        requireBranch.set("")
        pushToRemote.set("origin")
    }
}

//release {
//    buildTasks = ['publish', ":lionweb-gen-gradle:publishPlugins"]
//    git {
//        requireBranch.set('')
//        pushToRemote.set('origin')
//    }
//}

tasks.wrapper {
    gradleVersion = "8.2.1"
    distributionType = Wrapper.DistributionType.ALL
}

tasks.named(":publish") {
    finalizedBy(triggerSonatypePublish)
}

val isSnapshot = version.toString().endsWith("-SNAPSHOT")

val triggerSonatypePublish by tasks.registering {
    group = "publishing"
    description = "Triggers manual upload completion on central.sonatype.com"

    val username = project.findProperty("mavenCentralUsername") as? String ?: error("Missing username")
    val password = project.findProperty("mavenCentralPassword") as? String ?: error("Missing password")
    val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())
    val namespace = "com.strumenta"

    onlyIf { !isSnapshot }

    doLast {
        println("🚀 Triggering Sonatype publish for namespace $namespace")

        val url = "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/$namespace"
        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer $credentials")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        println("📦 Status: ${response.statusCode()}")
        println("🔸 Body: ${response.body()}")

        if (response.statusCode() !in 200..299) {
            throw GradleException("❌ Failed to trigger Sonatype publish: HTTP ${response.statusCode()}")
        }
    }
}

val dropClosedRepositories by tasks.registering {
    group = "publishing"
    description = "Drops all closed staging repos in new Sonatype Central"

    val username = project.findProperty("mavenCentralUsername") as? String ?: error("Missing username")
    val password = project.findProperty("mavenCentralPassword") as? String ?: error("Missing password")
    val credentials = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

    doLast {
        val namespace = "com.strumenta"
        val client = HttpClient.newHttpClient()

        val searchUrl = "https://ossrh-staging-api.central.sonatype.com/manual/search/repositories?ip=any&profile_id=$namespace"

        val searchRequest = HttpRequest.newBuilder()
            .uri(URI.create(searchUrl))
            .header("Authorization", "Bearer $credentials")
            .GET()
            .build()

        val searchResponse = client.send(searchRequest, HttpResponse.BodyHandlers.ofString())
        val body = searchResponse.body()

        val repoRegex = Regex("\"repositoryKey\"\\s*:\\s*\"(.*?)\"")
        val closedRepos = repoRegex.findAll(body).map { it.groupValues[1] }.toList()

        if (closedRepos.isEmpty()) {
            println("✅ Nessun repo chiuso da droppare.")
        } else {
            closedRepos.forEach { repoKey ->
                println("🧹 Dropping $repoKey")
                val dropUrl = "https://ossrh-staging-api.central.sonatype.com/manual/drop/repository/$repoKey"
                val dropRequest = HttpRequest.newBuilder()
                    .uri(URI.create(dropUrl))
                    .header("Authorization", "Bearer $credentials")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build()

                val dropResponse = client.send(dropRequest, HttpResponse.BodyHandlers.ofString())
                println("🔻 Drop status: ${dropResponse.statusCode()} — ${dropResponse.body()}")
            }
        }
    }
}