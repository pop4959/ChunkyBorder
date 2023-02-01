plugins {
    id("fabric-loom") version "0.12-SNAPSHOT"
}

val shade: Configuration by configurations.creating

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
    minecraft(group = "com.mojang", name = "minecraft", version = "1.19.3")
    mappings(group = "net.fabricmc", name = "yarn", version = "1.19.3+build.2", classifier = "v2")
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = "0.14.6")
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-api", version = "0.68.1+1.19.3")
    modImplementation(group = "org.popcraft", name = "chunky-fabric", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                "id" to rootProject.name,
                "version" to project.version,
                "name" to rootProject.name.capitalize(),
                "description" to project.property("description"),
                "author" to project.property("author"),
                "github" to project.property("github"),
                "target" to project.property("target")
            )
        }
    }
    shadowJar {
        configurations = listOf(shade)
        archiveClassifier.set("dev")
        archiveFileName.set(null as String?)
    }
    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveFileName.set("ChunkyBorder-${project.version}.jar")
    }
}
