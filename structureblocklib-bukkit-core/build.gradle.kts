import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.net.URL
import java.nio.file.Files
import java.util.*

plugins {
    id("com.github.johnrengelman.shadow") version ("7.0.0")
}

tasks.withType<ShadowJar> {
    dependsOn("jar")
    archiveName = "$baseName-$version.$extension"

    relocate("org.intellij", "com.github.shynixn.structureblocklib.lib.org.intellij")
    relocate("org.jetbrains", "com.github.shynixn.structureblocklib.lib.org.jetbrains")
}

tasks.register("pluginJar", Exec::class.java) {
    dependsOn("shadowJar")
    workingDir = buildDir

    if (!workingDir.exists()) {
        workingDir.mkdir();
    }

    val folder = File(workingDir, "mapping")

    if (!folder.exists()) {
        folder.mkdir()
    }

    val file = File(folder, "SpecialSources.jar")

    if (!file.exists()) {
        URL("https://repo.maven.apache.org/maven2/net/md-5/SpecialSource/1.10.0/SpecialSource-1.10.0-shaded.jar").openStream()
            .use {
                Files.copy(it, file.toPath())
            }
    }

    val shadowJar = tasks.findByName("shadowJar")!! as ShadowJar
    val obfArchiveName = "${shadowJar.baseName}-${shadowJar.version}-obfuscated.${shadowJar.extension}"
    val archiveName = "${shadowJar.baseName}-${shadowJar.version}.${shadowJar.extension}"
    val sourceJarFile = File(buildDir, "libs/" + shadowJar.archiveName)
    val obfJarFile = File(buildDir, "libs/$obfArchiveName")
    val targetJarFile = File(buildDir, "libs/$archiveName")

    val obsMapping =
        "java -jar ${file.absolutePath} -i \"$sourceJarFile\" -o \"$obfJarFile\" -m \"\$HOME/.m2/repository/org/spigotmc/minecraft-server/1.17-R0.1-SNAPSHOT/minecraft-server-1.17-R0.1-SNAPSHOT-maps-mojang.txt\" --reverse" +
                "&& java -jar ${file.absolutePath} -i \"$obfJarFile\" -o \"$targetJarFile\" -m \"\$HOME/.m2/repository/org/spigotmc/minecraft-server/1.17-R0.1-SNAPSHOT/minecraft-server-1.17-R0.1-SNAPSHOT-maps-spigot.csrg\""

    if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows")) {
        commandLine = listOf("cmd", "/c", obsMapping.replace("\$HOME", "%userprofile%"))
    } else {
        commandLine = listOf("sh", "-c", obsMapping)
    }
}

dependencies {
    implementation(project(":structureblocklib-api"))
    implementation(project(":structureblocklib-core"))
    implementation(project(":structureblocklib-bukkit-api"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-109R2"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-110R1"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-111R1"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-112R1"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-113R2"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-114R1"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-115R1"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-116R3"))
    implementation(project(":structureblocklib-bukkit-core:bukkit-nms-117R1"))

    compileOnly("org.spigotmc:spigot:1.14.4-R0.1-SNAPSHOT")
    testCompile("org.spigotmc:spigot:1.12-R0.1-SNAPSHOT")
}
