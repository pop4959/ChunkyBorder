repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
}

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
