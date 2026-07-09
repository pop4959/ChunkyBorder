plugins {
    id("net.fabricmc.fabric-loom") version "1.17.13"
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

loom {
    splitEnvironmentSourceSets()
    mods {
        create(rootProject.name) {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

dependencies {
    minecraft("com.mojang:minecraft:26.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:0.154.2+26.2")
    implementation("org.popcraft:chunky-fabric:${project.property("target_fabric")}")
    compileOnly(group = "us.dynmap", name = "DynmapCoreAPI", version = "${project.property("target_dynmap")}")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "${project.property("target_bluemap")}")
    compileOnly(group = "xyz.jpenilla", name = "squaremap-api", version = "${project.property("target_squaremap")}")
    compileOnly(group = "maven.modrinth", name = "pl3xmap", version = "${project.property("target_pl3xmap")}")
    implementation(project(":chunkyborder-common"))
    shade(project(":chunkyborder-common"))
}

tasks {
    withType<JavaCompile> {
        options.release = 25
    }
    processResources {
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to rootProject.name,
                    "version" to project.version.toString(),
                    "name" to project.property("artifactName").toString(),
                    "description" to project.property("description").toString(),
                    "author" to project.property("author").toString(),
                    "github" to project.property("github").toString(),
                    "target" to project.property("target_fabric").toString()
                )
            )
        }
    }
    shadowJar {
        dependsOn("clientClasses")
        from(sourceSets["client"].output)
        configurations = listOf(shade)
        archiveClassifier.set("dev")
        archiveFileName.set("${project.property("artifactName")}-Fabric-${project.version}.jar")
    }
}
