plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.33.0"
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("strumentaMavenPublish") {
            id = "com.strumenta.starlasu.build.plugin"
            implementationClass = "com.strumenta.starlasu.build.StarlasuBuildPlugin"
        }
    }
}

dependencies {
    implementation("com.vanniktech:gradle-maven-publish-plugin:0.33.0")
}
