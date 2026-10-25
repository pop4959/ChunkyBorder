plugins {
    id("dev.architectury.loom") version "1.17-SNAPSHOT"
}

val shade = configurations.create("shade")

repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.4")
    mappings("net.fabricmc:yarn:1.21.4+build.8:v2")
    modImplementation("net.fabricmc:fabric-loader:0.16.7")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.111.0+1.21.4")
    modImplementation("org.popcraft:chunky-fabric:${project.property("target")}")
    compileOnly("us.dynmap:DynmapCoreAPI:${project.property("target_dynmap")}")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:${project.property("target_bluemap")}")
    compileOnly("xyz.jpenilla:squaremap-api:${project.property("target_squaremap")}")
    compileOnly("maven.modrinth:pl3xmap:${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

tasks {
    processResources {
        val properties = mapOf(
            "id" to rootProject.name,
            "version" to project.version,
            "name" to project.property("artifactName")!!,
            "description" to project.property("description")!!,
            "author" to project.property("author")!!,
            "github" to project.property("github")!!,
            "target" to project.property("target")!!
        )
        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }
    shadowJar {
        configurations = listOf(shade)
        archiveClassifier.set("dev")
        archiveFileName.set(null as String?)
    }
    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveFileName.set("${project.property("artifactName")}-Fabric-${project.version}.jar")
    }
}
