val cliktVersion = extra["clikt_version"]
val gsonVersion = extra["gson_version"]

dependencies {
    implementation(project(":core"))
    implementation(project(":serialization"))

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")
    api("com.github.ajalt.clikt:clikt:$cliktVersion")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_cli", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_cli"])
}
