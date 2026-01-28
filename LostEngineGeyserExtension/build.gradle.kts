plugins {
    id("java")
}

group = "dev.lost"
version = "1.0-SNAPSHOT"
val id = "lostenginegeyserextension"
val extensionName = "LostEngineGeyserExtension"
val author = "Misieur"
val geyserApiVersion = "2.9.2"

repositories {
    mavenCentral()
    maven("https://repo.misieur.me/repository")
    // Tell Gradle to use OpenCollab's repo for some dependencies not provided by https://repo.misieur.me/repository
    maven("https://repo.opencollab.dev/main/") {
        content {
            includeGroup("org.geysermc.api")
            includeGroup("org.cloudburstmc.math")
            includeGroup("org.geysermc.cumulus")
            includeGroup("org.geysermc.event")
        }
    }
}

dependencies {
    compileOnly("org.geysermc.geyser:api:2.9.2-SNAPSHOT")
    // Using the same versions as Geyser for compatibility
    compileOnly("it.unimi.dsi:fastutil:8.5.15")
    compileOnly("com.google.code.gson:gson:2.3.1")

    compileOnly("org.jetbrains:annotations:26.0.2-1")

}

tasks {
    processResources {
        filesMatching("extension.yml") {
            expand(
                "id" to id,
                "name" to extensionName,
                "api" to geyserApiVersion,
                "version" to version,
                "author" to author
            )
        }
    }
}