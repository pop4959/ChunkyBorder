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
    minecraft("com.mojang:minecraft:1.21.6")
    mappings(loom.officialMojangMappings())
    forge("net.minecraftforge:forge:1.21.6-56.0.0")
    modImplementation("org.popcraft:chunky-forge:${project.property("target")}")
    compileOnly("us.dynmap:DynmapCoreAPI:${project.property("target_dynmap")}")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:${project.property("target_bluemap")}")
    compileOnly("xyz.jpenilla:squaremap-api:${project.property("target_squaremap")}")
    compileOnly("maven.modrinth:pl3xmap:${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
    components {
        withModule("org.popcraft:chunky-forge") {
            listOf("apiElements", "runtimeElements").forEach { variant ->
                withVariant(variant) {
                    withFiles {
                        removeAllFiles()
                        addFile("chunky-forge-${id.version}.jar")
                    }
                }
            }
        }
    }
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
        filesMatching("META-INF/mods.toml") {
            expand(properties)
        }
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to rootProject.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Vendor" to project.property("author"),
                    "MixinConfigs" to "chunkyborder.mixins.json"
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
