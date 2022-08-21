repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.18.2-R0.1-SNAPSHOT")
    compileOnly(group = "org.popcraft", name = "chunky-bukkit", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.3")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v2.1.0")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "1.1.2")
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.0")
    implementation(project(":chunkyborder-common"))
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand(
                "version" to project.version,
                "group" to project.group,
                "author" to project.property("author"),
                "description" to project.property("description")
            )
        }
    }
    shadowJar {
        minimize()
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.bstats")
    }
}
