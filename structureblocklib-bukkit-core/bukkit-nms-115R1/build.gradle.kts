dependencies {
    implementation(project(":structureblocklib-api"))
    implementation(project(":structureblocklib-core"))
    implementation(project(":structureblocklib-bukkit-api"))
    compileOnly("org.spigotmc:spigot:1.15-R0.1-SNAPSHOT")
    testCompile("org.spigotmc:spigot:1.15-R0.1-SNAPSHOT")
}
