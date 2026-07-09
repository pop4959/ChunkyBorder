plugins {
    id("dev.architectury.loom-no-remap") version "1.17.491"
}

val shade: Configuration by configurations.creating

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    minecraft(group = "com.mojang", name = "minecraft", version = "26.2")
    neoForge(group = "net.neoforged", name = "neoforge", version = "26.2.0.9-beta")
    implementation(group = "org.popcraft", name = "chunky-neoforge", version = "${project.property("target_neoforge")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks {
    withType<JavaCompile> {
        options.release = 25
    }
    processResources {
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(
                mapOf(
                    "id" to rootProject.name,
                    "version" to project.version.toString(),
                    "name" to project.property("artifactName").toString(),
                    "description" to project.property("description").toString(),
                    "author" to project.property("author").toString(),
                    "github" to project.property("github").toString(),
                    "target" to project.property("target_neoforge").toString()
                )
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
        archiveClassifier.set("")
        archiveFileName.set("${project.property("artifactName")}-NeoForge-${project.version}.jar")
    }
}
