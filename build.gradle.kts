import org.gradle.api.Project

plugins {
    id("java-library")
    id("maven-publish")
    id("com.gradleup.shadow") version "9.5.1"
}

subprojects {
    plugins.apply("java-library")
    plugins.apply("maven-publish")
    plugins.apply("com.gradleup.shadow")

    group = "${project.property("group")}"
    version = "${project.property("version")}.${project.commitsSinceLastTag()}"

    repositories {
        mavenCentral()
        maven("https://repo.codemc.io/repository/maven-public/")
    }

    dependencies {
        compileOnly(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.14.1")
        compileOnly(group = "com.google.code.gson", name = "gson", version = "2.8.9")
        compileOnly(group = "org.popcraft", name = "chunky-common", version = "${project.property("target")}")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
        withSourcesJar()
    }

    tasks {
        withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release = 21
        }
        jar {
            archiveClassifier.set("noshade")
        }
        shadowJar {
            archiveClassifier.set("")
            archiveFileName.set("${project.property("artifactName")}-${project.version}.jar")
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

fun Project.commitsSinceLastTag(): String {
    val tagDescription = providers.exec {
        commandLine("git", "describe", "--tags")
    }.standardOutput.asText.get()
    if (tagDescription.indexOf('-') < 0) {
        return "0"
    }
    return tagDescription.split('-')[1]
}
