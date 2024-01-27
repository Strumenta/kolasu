val cliktVersion = extra["clikt_version"]
val gsonVersion = extra["gson_version"]

dependencies {
    api("com.badoo.reaktive:reaktive:2.0.1")
}

publishing {
    addSonatypeRepo(project)
    addPublication("kolasu_ast", "Framework to work with AST and building languages", project)
}

signing {
    sign(publishing.publications["kolasu_ast"])
}
