repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    maven {
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
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
