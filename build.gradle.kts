plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("maven-publish")
    id("signing")
}

group = "com.alazeprt"
version = "1.2"
description = "Quickly and easily import dependencies locally"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.maven:maven-model:3.9.4")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.javadoc {
    exclude("/com/alazeprt/Test.java")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            project.group = groupId
            project.version = version
            from(components["java"])
            pom {
                name.set("JavaDependencyManager")
                description.set("Quickly and easily import dependencies locally")
                url.set("https://github.com/alazeprt/JavaDependencyManager")
                licenses {
                    license {
                        name.set("GPL-3.0")
                        url.set("https://spdx.org/licenses/GPL-3.0.html")
                    }
                }
                developers {
                    developer {
                        name.set(properties.getting("sonatypeUsername"))
                        email.set(properties.getting("sonatypeEmail"))
                        timezone.set(properties.getting("sonatypeTimezone"))
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/alazeprt/JavaDependencyManager.git")
                    url.set("https://github.com/alazeprt/JavaDependencyManager")
                    developerConnection.set("scm:git:https://github.com/alazeprt/JavaDependencyManager.git")
                }
            }
        }
    }
    repositories {
        maven {
            name = "JavaDependencyManager"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = properties["sonatypeUsername"].toString()
                password = properties["sonatypePassword"].toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
}