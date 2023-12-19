val clikt_version = extra["clikt_version"]
val gson_version = extra["gson_version"]

dependencies {
    implementation(project(":core"))
    implementation(project(":serialization"))

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")
    api("com.github.ajalt.clikt:clikt:$clikt_version")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_cli", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_cli"])
}
