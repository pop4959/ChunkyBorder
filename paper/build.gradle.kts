repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.mikeprimm.com")
    maven("https://jitpack.io")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.186-stable")
    compileOnly("org.popcraft:chunky-paper:1.5.20")
    compileOnly("us.dynmap:DynmapCoreAPI:${project.property("target_dynmap")}")
    compileOnly("com.github.BlueMap-Minecraft:BlueMapAPI:${project.property("target_bluemap")}")
    compileOnly("xyz.jpenilla:squaremap-api:${project.property("target_squaremap")}")
    compileOnly("maven.modrinth:pl3xmap:${project.property("target_pl3xmap")}")
    implementation("org.bstats:bstats-bukkit:3.2.1")
    implementation(project(":chunkyborder-common"))
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
        val properties = mapOf(
            "version" to project.version,
            "group" to project.group,
            "author" to project.property("author")!!,
            "description" to project.property("description")!!
        )
        inputs.properties(properties)
        filesMatching("plugin.yml") {
            expand(properties)
        }
    }
    shadowJar {
        minimize()
        relocate("org.bstats", "${project.group}.${rootProject.name}.lib.bstats")
        manifest {
            attributes("paperweight-mappings-namespace" to "mojang")
        }
        archiveFileName.set("${project.property("artifactName")}-Paper-${project.version}.jar")
    }
}
