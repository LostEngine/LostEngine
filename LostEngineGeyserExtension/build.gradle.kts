plugins {
    id("java")
}

group = "dev.lost"
version = "1.0-SNAPSHOT"
val id = "lostenginegeyserextension"
val extensionName = "LostEngineGeyserExtension"
val author = "Misieur"
val geyserApiVersion = "2.9.3"

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
}

dependencies {
    compileOnly("org.geysermc.geyser:api:2.9.3-SNAPSHOT")
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