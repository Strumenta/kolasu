val clikt_version = extra["clikt_version"]

dependencies {
    implementation("com.github.salomonbrys.kotson:kotson:2.5.0")
    implementation("org.redundent:kotlin-xml-builder:1.9.0")
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.8.0")

    implementation("com.github.ajalt.clikt:clikt:$clikt_version")
    implementation("io.reactivex.rxjava3:rxjava:3.1.1")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_core", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_core"])
}
