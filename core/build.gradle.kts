dependencies {
    api("io.reactivex.rxjava3:rxjava:3.1.1")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_core", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_core"])
}
