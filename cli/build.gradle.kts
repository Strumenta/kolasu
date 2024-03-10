dependencies {
    implementation(project(":core"))
    implementation(project(":ast"))
    implementation(project(":serialization"))

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")
    api(libs.clikt)
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_cli", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_cli"])
}
