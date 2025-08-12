plugins {
    id("dev.architectury.loom") version "1.10-SNAPSHOT"
}

val shade: Configuration by configurations.creating

repositories {
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = "1.21.4")
    mappings(loom.officialMojangMappings())
    forge(group = "net.minecraftforge", name = "forge", version = "1.21.4-54.0.0")
    modImplementation(group = "org.popcraft", name = "chunky-forge", version = "${project.property("target")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

tasks {
    processResources {
        filesMatching("META-INF/mods.toml") {
            expand(
                "id" to rootProject.name,
                "version" to project.version,
                "name" to project.property("artifactName"),
                "description" to project.property("description"),
                "author" to project.property("author"),
                "github" to project.property("github"),
                "target" to project.property("target")
            )
        }
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to project.property("author")
                )
            )
        }
    }
    shadowJar {
        configurations = listOf(shade)
        archiveClassifier.set(null as String?)
        archiveFileName.set("${project.property("artifactName")}-Forge-${project.version}.jar")
    }
    remapJar {
        enabled = false
    }
}
