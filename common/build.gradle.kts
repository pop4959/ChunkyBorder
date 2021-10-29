tasks {
    processResources {
        filesMatching("version.properties") {
            expand(
                "version" to project.version,
                "target" to project.property("target")
            )
        }
    }
}
