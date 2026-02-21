import com.github.gradle.node.npm.task.NpmTask
import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    java
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("com.gradleup.shadow") version "9.2.2"
    id("com.github.node-gradle.node") version "7.1.0"
}

group = "dev.misieur"
version = "0.0.5-mc1.21.11"

configurations {
    create("proguard")
}

repositories {
    mavenCentral()
    maven {
        name = "papermc-repo"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven("https://repo.opencollab.dev/main/")
    maven {
        url = uri("https://repo.misieur.me/repository")
        content {
            includeGroup("dev.misieur")
        }
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    implementation(project(":furnace"))
    implementation("dev.misieur:fast:1.0.2")
    implementation("dev.misieur:justamaterial:1.0-SNAPSHOT")

    compileOnly("org.geysermc.geyser:api:2.9.3-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")

    "proguard"("com.guardsquare:proguard-base:7.8.2")
}

runPaper.folia.registerTask()

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

node {
    download = true
    version = "22.18.0"
}

val buildNpm by tasks.registering(NpmTask::class) {
    dependsOn(installNpm)
    workingDir.set(file("webeditor"))
    args.set(listOf("run", "build"))
}

val installNpm by tasks.registering(NpmTask::class) {
    workingDir.set(file("webeditor"))
    args.set(listOf("install"))
}

tasks.processResources {
    dependsOn(buildNpm)
    from("webeditor/dist") {
        include("*.html")
        into("generated")
    }
    from("LICENSE.MD") {
        into("META-INF")
    }

    filesMatching("paper-plugin.yml") {
        filter<ReplaceTokens>("tokens" to mapOf("version" to project.version.toString()))
    }
}

tasks.build {
    dependsOn(proguardJar)
}

tasks.shadowJar {
    archiveClassifier.set("unminimized")
    destinationDirectory.set(layout.buildDirectory.dir("tmp/libs"))
    finalizedBy(proguardJar)
}


val proguardJar by tasks.registering(JavaExec::class) {
    group = "build"

    dependsOn(tasks.shadowJar)

    classpath = configurations["proguard"]
    mainClass.set("proguard.ProGuard")

    val inputJar = tasks.shadowJar.get().archiveFile.get().asFile
    val outputJar = layout.buildDirectory.file("libs/${project.name}-${project.version}.jar")

    args(
        "-injars", inputJar.absolutePath,
        "-outjars", outputJar.get().asFile.absolutePath,
        "@${project.projectDir}/.config/proguard-rules.pro"
    )
}