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
    compileOnly("org.spigotmc:spigot-api:1.21.2-R0.1-SNAPSHOT")
    compileOnly("org.popcraft:chunky-bukkit:${project.property("target")}")
    compileOnly("org.popcraft:chunky-folia:${project.property("target")}")
    compileOnly("us.dynmap:DynmapCoreAPI:${project.property("target_dynmap")}")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:${project.property("target_bluemap")}")
    compileOnly("xyz.jpenilla:squaremap-api:${project.property("target_squaremap")}")
    compileOnly("maven.modrinth:pl3xmap:${project.property("target_pl3xmap")}")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation(project(":chunkyborder-common"))
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "group" to project.group,
            "author" to project.property("author")!!,
            "description" to project.property("description")!!
        )
        inputs.properties(properties)
        filesMatching("plugin.yml") {
            expand(properties)
        }
    }
    shadowJar {
        minimize()
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.bstats")
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
        archiveFileName.set("${project.property("artifactName")}-Bukkit-${project.version}.jar")
    }
}
