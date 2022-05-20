plugins {
    id("fabric-loom") version "0.11-SNAPSHOT"
}

val shade: Configuration by configurations.creating

repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = "1.18")
    mappings(group = "net.fabricmc", name = "yarn", version = "1.18+build.1", classifier = "v2")
    modImplementation(group = "net.fabricmc", name = "fabric-loader", version = "0.12.6")
    modImplementation(group = "net.fabricmc.fabric-api", name = "fabric-api", version = "0.43.1+1.18")
    modImplementation(group = "org.popcraft", name = "chunky-fabric", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "3.3")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v1.7.0")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "1.1.2")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
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
