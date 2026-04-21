plugins {
    id("java")
}

group = "dev.lost"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
