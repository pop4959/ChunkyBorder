repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.3")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v2.1.0")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "1.1.2")
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
