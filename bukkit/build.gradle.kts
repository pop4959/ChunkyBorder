repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.20.5-R0.1-SNAPSHOT")
    compileOnly(group = "org.popcraft", name = "chunky-bukkit", version = "${project.property("target")}")
    compileOnly(group = "org.popcraft", name = "chunky-folia", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
    implementation(group = "org.bstats", name = "bstats-bukkit", version = "3.0.2")
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
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
    }
}
