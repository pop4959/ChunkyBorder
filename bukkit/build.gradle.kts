repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.mikeprimm.com")
    maven("https://repo.pl3x.net")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.16.4-R0.1-SNAPSHOT")
    compileOnly(group = "org.popcraft", name = "chunky-bukkit", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "dynmap-api", version = "3.0")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v1.3.0")
    compileOnly(group = "net.pl3x.map", name = "pl3xmap-api", version = "1.0.0-SNAPSHOT")
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "2.2.1")
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
