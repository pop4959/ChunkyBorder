val pl3xmap: String by project

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
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.3")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v2.1.0")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "1.1.2")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = pl3xmap)
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
