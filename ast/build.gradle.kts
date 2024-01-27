val cliktVersion = extra["clikt_version"]
val gsonVersion = extra["gson_version"]

dependencies {
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_ast", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_ast"])
}
