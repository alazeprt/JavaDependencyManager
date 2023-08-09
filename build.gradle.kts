plugins {
    id("java")
    // shadow
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.alazeprt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.maven:maven-model:3.9.4")
}