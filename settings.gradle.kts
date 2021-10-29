rootProject.name = "chunkyborder"

sequenceOf(
    "common",
    "bukkit"
).forEach {
    include("${rootProject.name}-$it")
    project(":${rootProject.name}-$it").projectDir = file(it)
}
