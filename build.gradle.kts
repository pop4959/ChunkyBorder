import java.io.ByteArrayOutputStream

plugins {
    id("java-library")
    id("maven-publish")
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

allprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.github.johnrengelman.shadow")

    group = "${project.property("group")}"
    version = "${project.property("version")}.${commitsSinceLastTag()}"

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(8))
        }
        withSourcesJar()
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
        }
        jar {
            archiveClassifier.set("noshade")
        }
        shadowJar {
            archiveClassifier.set("")
            archiveFileName.set("ChunkyBorder-${project.version}.jar")
        }
        build {
            dependsOn(shadowJar)
        }
    }

    publishing {
        repositories {
            if (project.hasProperty("mavenUsername") && project.hasProperty("mavenPassword")) {
                maven {
                    credentials {
                        username = "${project.property("mavenUsername")}"
                        password = "${project.property("mavenPassword")}"
                    }
                    url = uri("https://repo.codemc.io/repository/maven-releases/")
                }
            }
        }
        publications {
            create<MavenPublication>("maven") {
                groupId = "${project.group}"
                artifactId = project.name
                version = "${project.version}"
                from(components["java"])
            }
        }
    }
}

fun commitsSinceLastTag(): String {
    val tagDescription = ByteArrayOutputStream()
    exec {
        commandLine("git", "describe", "--tags")
        standardOutput = tagDescription
    }
    if (tagDescription.toString().indexOf('-') < 0) {
        return "0"
    }
    return tagDescription.toString().split('-')[1]
}

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.mikeprimm.com")
    maven("https://repo.pl3x.net")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly(group = "org.spigotmc", name = "spigot-api", version = "1.16.4-R0.1-SNAPSHOT")
    compileOnly(group = "org.popcraft", name = "chunky-common", version = "1.2.98")
    compileOnly(group = "org.popcraft", name = "chunky-bukkit", version = "1.2.98")
    compileOnly(group = "us.dynmap", name = "dynmap-api", version = "3.0")
    compileOnly(group = "com.github.BlueMap-Minecraft", name = "BlueMapAPI", version = "v1.3.0")
    compileOnly(group = "net.pl3x.map", name = "pl3xmap-api", version = "1.0.0-SNAPSHOT")
    implementation(group = "io.papermc", name = "paperlib", version = "1.0.6")
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
        relocate("io.papermc.lib", "${project.group}.${rootProject.name}.lib.paperlib")
    }
}
