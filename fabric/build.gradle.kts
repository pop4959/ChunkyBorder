plugins {
    id("fabric-loom") version "0.12-SNAPSHOT"
}

val shade: Configuration by configurations.creating
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
    minecraft(group = "com.mojang", name = "minecraft", version = "1.19")
    mappings(group = "net.fabricmc", name = "yarn", version = "1.19+build.4", classifier = "v2")
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = "0.14.6")
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-api", version = "0.58.0+1.19")
    modImplementation(group = "org.popcraft", name = "chunky-fabric", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.3")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v2.1.0")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "1.1.2")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = pl3xmap)
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
